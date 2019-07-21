package com.unistudents.api.web;

import com.unistudents.api.model.GradeResults;
import com.unistudents.api.service.GradesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GradesController {

    @Autowired
    private GradesService gradesService;

    @GetMapping("/grades")
    public GradeResults getGradeResults(@RequestParam String username, @RequestParam String pass) {
        return gradesService.getGrades(username, pass);
    }
}
