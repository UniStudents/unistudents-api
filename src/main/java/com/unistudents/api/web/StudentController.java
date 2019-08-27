package com.unistudents.api.web;

import com.unistudents.api.model.LoginForm;
import com.unistudents.api.model.Student;
import com.unistudents.api.model.StudentObj;
import com.unistudents.api.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @PostMapping("/student")
    public ResponseEntity<Student> getStudent(@RequestBody LoginForm loginForm) {

        // get student object from injected service
        Student student = studentService.getStudent(loginForm.getUsername(), loginForm.getPassword());

        if (student == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        else {
            return new ResponseEntity<>(student, HttpStatus.OK);
        }
    }
}
