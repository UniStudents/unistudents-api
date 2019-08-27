package com.unistudents.api.web;

import com.unistudents.api.model.Grades;
import com.unistudents.api.model.LoginForm;
import com.unistudents.api.service.GradesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GradesController {

    @Autowired
    private GradesService gradesService;

    @PostMapping("/grades")
    public ResponseEntity<Grades> getGrades(@RequestBody LoginForm loginForm) {

        Grades grades = gradesService.getGrades(loginForm.getUsername(), loginForm.getPassword());

        if (grades == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        else {
            return new ResponseEntity<>(grades, HttpStatus.OK);
        }
    }
}
