package com.unistudents.api.controllers;

import com.unistudents.api.components.LoginForm;
import com.unistudents.api.services.ParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parser")
@EnableCaching
public class ParserController {
    @Autowired
    private ParserService parser;

    @RequestMapping(value = {"/{university}"}, method = RequestMethod.POST)
    public ResponseEntity getStudent(
            @PathVariable("university") String university,
            @RequestBody String body
    ) {
        System.out.println("HIT ENDPOINT");
        return parser.get(university, body);
    }
}
