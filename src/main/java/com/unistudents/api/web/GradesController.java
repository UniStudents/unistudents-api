package com.unistudents.api.web;

import com.unistudents.api.model.GradeResults;
import com.unistudents.api.model.LoginForm;
import com.unistudents.api.service.GradesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GradesController {

    @Autowired
    private GradesService gradesService;

    @PostMapping("/grades")
    public ResponseEntity<GradeResults> getGradeResults(@RequestBody LoginForm loginForm) {

        GradeResults grades = gradesService.getGrades(loginForm.getUsername(), loginForm.getPassword());

        if (grades == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        else {
            return new ResponseEntity<>(grades, HttpStatus.OK);
        }
    }
}
