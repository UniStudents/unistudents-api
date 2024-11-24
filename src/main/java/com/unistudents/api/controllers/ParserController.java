package com.unistudents.api.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.unistudents.api.services.ParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parser")
@EnableCaching
public class ParserController {
    @Autowired
    private ParserService parser;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity parse(@RequestBody JsonNode body) throws Exception {
        String university = body.has("university") ? body.get("university").asText() : null;
        String system = body.has("system") ? body.get("system").asText() : null;
        JsonNode output = body.has("output") ? body.get("output") : null;

        return parser.parse(university, system, output);
    }
}
