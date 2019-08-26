package com.unistudents.api.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GradesController {

//    @PostMapping("/grades")
//    public ResponseEntity<Grades> getGrades(@RequestBody LoginForm loginForm) {
//
//        Grades grades = gradesService.getGrades(loginForm.getUsername(), loginForm.getPassword());
//
//        if (grades == null) {
//            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
//        }
//        else {
//            return new ResponseEntity<>(grades, HttpStatus.OK);
//        }
//    }
}
