package demo.ms.todo.controller;

import com.google.common.collect.Lists;
import demo.ms.common.entity.Card;
import demo.ms.common.entity.Message;
import demo.ms.common.entity.Todo;
import demo.ms.common.entity.User;
import demo.ms.common.vo.JwtUserInfo;
import demo.ms.todo.repository.CardRepository;
import demo.ms.todo.repository.TodoRepository;
import demo.ms.todo.stream.LoggerEventSink;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;

@RequestMapping("/todos")
@RestController
@EnableBinding(LoggerEventSink.class)
public class TodoController {

    @Autowired
    TodoRepository todoRepository;

    @Autowired
    CardRepository cardRepository;

    @Autowired
    LoggerEventSink loggerEventSink;

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/{id:\\d+}")
    public ResponseEntity find(@PathVariable Integer id){
        Todo todo = todoRepository.findOne(id);
        if(todo==null){
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity(todo,HttpStatus.OK);
    }

    @PostMapping
    public Todo create(@RequestBody Todo todo) throws Exception {
        JwtUserInfo jwtUserInfo = (JwtUserInfo)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(todo.getCardId() == null){
            throw new Exception("cardId is required!");
        }
        todoRepository.save(todo);

        Card card = cardRepository.findOne(todo.getCardId());

        Message msg = new Message(
                null,
                null,
                Long.valueOf(card.getProjectId()),
                "PROJECT",
                String.format("[%s]创建了任务[%s]",jwtUserInfo.getLoginName(),todo.getTitle()),new Date(System
                .currentTimeMillis()));
        loggerEventSink.output().send(MessageBuilder.withPayload(msg).build());

        return todo;
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity update(@PathVariable Integer id, @RequestBody Todo todo){
        JwtUserInfo jwtUserInfo = (JwtUserInfo)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Todo oldTodo = todoRepository.findOne(id);

        Card oldCard = cardRepository.findOne(oldTodo.getCardId());

        List<Message> messageList = Lists.newArrayList();

        if(oldTodo == null){
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        if(todo.getUid()!=null){
            User user = restTemplate.getForEntity("http://USER-SERVICE/users/" + todo.getUid(),User.class).getBody();
            if(user!=null){
                oldTodo.setUid(todo.getUid());
                Message msg1 = new Message(
                        null,
                        Long.valueOf(todo.getUid()),
                        Long.valueOf(oldCard.getProjectId()),
                        "PROJECT",
                        String.format("[%s]分配任务[%s]给你",jwtUserInfo.getLoginName(),oldTodo.getTitle()),new Date(System
                        .currentTimeMillis()));
                Message msg2 = new Message(
                        null,
                        null,
                        Long.valueOf(oldCard.getProjectId()),
                        "PROJECT",
                        String.format("[%s]分配任务[%s]给[%s]",jwtUserInfo.getLoginName(),oldTodo.getTitle(),user.getUsername()),new Date
                        (System
                        .currentTimeMillis()));
                messageList.add(msg1);
                messageList.add(msg2);
            }else{
                return new ResponseEntity("该用户不存在",HttpStatus.NOT_FOUND);
            }
        }

        if(StringUtils.isNotEmpty(todo.getTitle())){
            oldTodo.setTitle(todo.getTitle());
        }

        if(todo.getDate()!=null){
            Message msg = new Message(
                    null,
                    null,
                    Long.valueOf(oldCard.getProjectId()),
                    "PROJECT",
                    String.format("[%s]将任务[%s]时间修改为[%s]",jwtUserInfo.getLoginName(),oldTodo.getTitle(),
                            new SimpleDateFormat("yyyy-mm-dd HH:mm:ss")),
                    new Date
                            (System
                                    .currentTimeMillis()));
            messageList.add(msg);
            oldTodo.setDate(todo.getDate());
        }

        if(todo.getCardId()!=null){
            Card card = cardRepository.getOne(todo.getCardId());
            Message msg = new Message(
                    null,
                    null,
                    Long.valueOf(oldCard.getProjectId()),
                    "PROJECT",
                    String.format("[%s]将任务[%s]从[%s]修改为[%s]",jwtUserInfo.getLoginName(),oldTodo.getTitle(),oldCard
                                    .getTitle(),card.getTitle()),
                    new Date(System.currentTimeMillis()));
            messageList.add(msg);
            oldTodo.setCardId(todo.getCardId());
        }

        todoRepository.save(oldTodo);

        messageList.stream().forEach(msg->loggerEventSink.output().send(MessageBuilder.withPayload(msg).build()));

        return new ResponseEntity(oldTodo,HttpStatus.OK);
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity delete(@PathVariable Integer id){
        JwtUserInfo jwtUserInfo = (JwtUserInfo)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Todo oldTodo = todoRepository.findOne(id);

        Card oldCard = cardRepository.findOne(oldTodo.getCardId());

        if(oldTodo==null){
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        todoRepository.delete(id);
        Message msg = new Message(
                null,
                null,
                Long.valueOf(oldCard.getProjectId()),
                "PROJECT",
                String.format("[%s]删除任务[%s]",jwtUserInfo.getLoginName(),oldTodo.getTitle()),
                new Date(System.currentTimeMillis()));
        loggerEventSink.output().send(MessageBuilder.withPayload(msg).build());
        return new ResponseEntity(HttpStatus.OK);
    }
}
