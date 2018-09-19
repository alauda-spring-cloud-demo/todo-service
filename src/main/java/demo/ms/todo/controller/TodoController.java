package demo.ms.todo.controller;

import demo.ms.common.vo.JwtUserInfo;
import demo.ms.todo.entity.Todo;
import demo.ms.todo.repository.TodoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/todos")
@RestController
public class TodoController {

    @Autowired
    TodoRepository todoRepository;

    @GetMapping("/{id}")
    public ResponseEntity find(@PathVariable Integer id){
        Todo todo = todoRepository.findOne(id);
        if(todo==null){
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity(todo,HttpStatus.OK);
    }

    @PostMapping
    public Todo create(@RequestBody Todo todo){
        JwtUserInfo jwtUserInfo =  (JwtUserInfo)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        todo.setUid(Integer.parseInt(jwtUserInfo.getUserId()));
        todoRepository.save(todo);
        return todo;
    }

    @PutMapping("/{id}")
    public ResponseEntity update(@PathVariable Integer id,@RequestBody Todo todo){
        if(!todoRepository.exists(id)){
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        todoRepository.save(todo);
        return new ResponseEntity(todo,HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable Integer id){
        if(!todoRepository.exists(id)){
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        todoRepository.delete(id);
        return new ResponseEntity(HttpStatus.OK);
    }
}
