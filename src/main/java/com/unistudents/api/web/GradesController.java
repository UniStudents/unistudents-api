package com.unistudents.api.web;

import com.unistudents.api.model.GradeResults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GradesController {

    // autowire scrap service

    @GetMapping("/grades")
    public GradeResults getGradeResults(@RequestParam String username, @RequestParam String pass) {

        return new GradeResults();
    }
}
