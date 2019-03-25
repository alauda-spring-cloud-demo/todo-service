package demo.ms.todo.controller;

import com.google.common.collect.Lists;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import demo.ms.common.entity.Card;
import demo.ms.common.entity.Message;
import demo.ms.common.entity.Todo;
import demo.ms.common.vo.JwtUserInfo;
import demo.ms.todo.repository.CardRepository;
import demo.ms.todo.repository.TodoRepository;
import demo.ms.todo.stream.LoggerEventSink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.util.List;

@RestController
public class CardController {

    @Autowired
    CardRepository cardRepository;

    @Autowired
    TodoRepository todoRepository;

    @Autowired
    LoggerEventSink loggerEventSink;


    @HystrixCommand(commandKey = "CreateCard")
    @PreAuthorize("hasAnyRole('ROLE_PMO','ROLE_ADMIN')")
    @PostMapping("/cards")
    public Card create(@RequestBody Card card) throws Exception {
        JwtUserInfo jwtUserInfo = (JwtUserInfo)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(card.getProjectId() == null){
            throw new Exception("projectId is required!");
        }
        cardRepository.save(card);

        Message msg = new Message(
                null,
                null,
                Long.valueOf(card.getProjectId()),
                "PROJECT",
                String.format("[%s]创建了卡片[%s]",jwtUserInfo.getLoginName(),card.getTitle()),new Date(System
                .currentTimeMillis()));
        loggerEventSink.output().send(MessageBuilder.withPayload(msg).build());

        return card;
    }


    @HystrixCommand(commandKey = "BatchCreateCard")
    @PostMapping("/cards/batch")
    public List<Card> batchCreate(@RequestBody List<Card> cardList){
        cardRepository.save(cardList);
        return cardList;
    }

    @HystrixCommand(commandKey = "UpdateCard")
    @PutMapping("/cards")
    public ResponseEntity update(@RequestBody Card card){

        Card oldCard = cardRepository.findOne(card.getId());

        JwtUserInfo jwtUserInfo = (JwtUserInfo)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(oldCard == null){
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        cardRepository.save(card);

        Message msg = new Message(
                null,
                null,
                Long.valueOf(card.getProjectId()),
                "PROJECT",
                String.format("[%s]将卡片[%s]修改为[%s]",jwtUserInfo.getLoginName(),oldCard.getTitle(),card.getTitle()),new Date
                (System
                .currentTimeMillis()));
        loggerEventSink.output().send(MessageBuilder.withPayload(msg).build());

        return new ResponseEntity(HttpStatus.OK);
    }

    @HystrixCommand(commandKey = "GetCardById")
    @GetMapping("/cards/{id:\\d+}")
    public ResponseEntity find(@PathVariable Long id){

        Card oldCard = cardRepository.findOne(id);
        if(oldCard == null){
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity(cardRepository.findOne(id),HttpStatus.OK);
    }

    @HystrixCommand(commandKey = "DeleteCardById")
    @PreAuthorize("hasAnyRole('ROLE_PMO','ROLE_ADMIN')")
    @Transactional
    @DeleteMapping("/cards/{id:\\d+}")
    public ResponseEntity delete(@PathVariable Long id){

        JwtUserInfo jwtUserInfo = (JwtUserInfo)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Card oldCard = cardRepository.findOne(id);

        if(!cardRepository.exists(id)){
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        Todo todoInCard = new Todo();
        todoInCard.setCardId(id);

        Example<Todo> todoExample = Example.of(todoInCard);

        todoRepository.delete(todoRepository.findAll(todoExample));
        cardRepository.delete(id);

        Message msg = new Message(
                null,
                null,
                Long.valueOf(oldCard.getProjectId()),
                "PROJECT",
                String.format("[%s]删除卡片[%s]",jwtUserInfo.getLoginName(),oldCard.getTitle()),new Date(System.currentTimeMillis()));

        loggerEventSink.output().send(MessageBuilder.withPayload(msg).build());

        return new ResponseEntity(HttpStatus.OK);
    }

    @HystrixCommand(commandKey = "ListCardsInProject")
    @GetMapping("/cards")
    public ResponseEntity list(String project){
        Long projectId = Long.parseLong(project);
        Card userCard = new Card();
        userCard.setProjectId(projectId);
        Example<Card> cardExample = Example.of(userCard);
        Sort sort = new Sort(Sort.Direction.ASC, Lists.newArrayList("id"));
        List<Card> cardList = cardRepository.findAll(cardExample,sort);
        return new ResponseEntity(cardList,HttpStatus.OK);
    }

    @HystrixCommand(commandKey = "ListTodosInCard")
    @GetMapping("/cards/{id:\\d+}/todos")
    public ResponseEntity listTodos(@PathVariable Long id){
        if(!cardRepository.exists(id)){
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity(cardRepository.findOne(id).getTodoList(),HttpStatus.OK);
    }
}
