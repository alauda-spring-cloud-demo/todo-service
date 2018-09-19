package demo.ms.todo.controller;

import com.google.common.collect.Lists;
import demo.ms.common.vo.JwtUserInfo;
import demo.ms.todo.entity.Card;
import demo.ms.todo.entity.Todo;
import demo.ms.todo.repository.CardRepository;
import demo.ms.todo.repository.TodoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/cards")
@RestController
public class CardController {

    @Autowired
    CardRepository cardRepository;

    @Autowired
    TodoRepository todoRepository;


    @PostMapping
    public Card create(@RequestBody Card card){
        JwtUserInfo jwtUserInfo =  (JwtUserInfo)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        card.setUid(Integer.parseInt(jwtUserInfo.getUserId()));
        cardRepository.save(card);
        return card;
    }

    @PutMapping("/{id}")
    public ResponseEntity update(@PathVariable Integer id, @RequestBody Card card){
        if(!cardRepository.exists(id)){
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        cardRepository.save(card);
        return new ResponseEntity(HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity find(@PathVariable Integer id){
        if(!cardRepository.exists(id)){
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity(cardRepository.findOne(id),HttpStatus.OK);
    }

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable Integer id){
        if(!cardRepository.exists(id)){
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        Todo todoInCard = new Todo();
        todoInCard.setCardId(id);

        Example<Todo> todoExample = Example.of(todoInCard);

        todoRepository.delete(todoRepository.findAll(todoExample));
        cardRepository.delete(id);
        return new ResponseEntity(HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity list(){
        JwtUserInfo jwtUserInfo =  (JwtUserInfo)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Card userCard = new Card();
        userCard.setUid(Integer.parseInt(jwtUserInfo.getUserId()));
        Example<Card> cardExample = Example.of(userCard);
        List<Card> cardList = cardRepository.findAll(cardExample);
        return new ResponseEntity(cardList,HttpStatus.OK);
    }

    @GetMapping("/{id}/todos")
    public ResponseEntity listTodos(@PathVariable Integer id){
        if(!cardRepository.exists(id)){
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity(cardRepository.findOne(id).getTodoList(),HttpStatus.OK);
    }
}
