package com.unistudents.api.web;

import com.unistudents.api.model.GradeResults;
import com.unistudents.api.model.LoginForm;
import com.unistudents.api.service.GradesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GradesController {

    @Autowired
    private GradesService gradesService;

    @PostMapping("/grades")
    public GradeResults getGradeResults(@RequestBody LoginForm loginForm) {
        return gradesService.getGrades(loginForm.getUsername(), loginForm.getUsername());
    }
}
