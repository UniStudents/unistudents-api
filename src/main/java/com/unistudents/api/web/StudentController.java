package com.unistudents.api.web;

import com.unistudents.api.model.LoginForm;
import com.unistudents.api.model.StudentObj;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StudentController {

    // autowire student service

    @PostMapping("/student")
    public ResponseEntity<StudentObj> getStudent(@RequestBody LoginForm loginForm) {

        // get student object from injected service

        // return object
        return null;
    }
}
