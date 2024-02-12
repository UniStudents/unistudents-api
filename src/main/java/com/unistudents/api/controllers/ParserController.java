package com.unistudents.api.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity parse(@RequestBody JsonNode body) throws Exception {
        String university = body.has("university") ? body.get("university").asText() : null;
        String system = body.has("system") ? body.get("system").asText() : null;
        JsonNode output = body.has("output") ? body.get("output") : null;

        return parser.parse(university, system, output);
    }
}
