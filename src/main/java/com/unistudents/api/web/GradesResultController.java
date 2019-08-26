package com.unistudents.api.web;

import com.unistudents.api.model.GradeResults;
import com.unistudents.api.model.LoginForm;
import com.unistudents.api.service.GradesResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GradesResultController {

    @Autowired
    private GradesResultService gradesResultService;

    @PostMapping("/grades")
    public ResponseEntity<GradeResults> getGradeResults(@RequestBody LoginForm loginForm) {

        GradeResults grades = gradesResultService.getGrades(loginForm.getUsername(), loginForm.getPassword());

        if (grades == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        else {
            return new ResponseEntity<>(grades, HttpStatus.OK);
        }
    }

//    @PostMapping("/grades")
//    public ResponseEntity<Grades> getGrades(@RequestBody LoginForm loginForm) {
//
//        Grades grades = gradesResultService.getGrades(loginForm.getUsername(), loginForm.getPassword());
//
//        if (grades == null) {
//            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
//        }
//        else {
//            return new ResponseEntity<>(grades, HttpStatus.OK);
//        }
//    }
}
