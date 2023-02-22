package com.unistudents.api.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.unistudents.api.components.LoginForm;
import com.unistudents.api.services.StudentServiceService;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student")
@EnableCaching
public class StudentServiceController {
    @Autowired
    private StudentServiceService student;

    @CrossOrigin
    @RequestMapping(value = {"/image"}, produces = MediaType.IMAGE_JPEG_VALUE, method = RequestMethod.POST)
    public @ResponseBody byte[] getImage(
            @RequestParam("url") String url,
            @RequestBody JsonNode jsonNode) {

        try {
            Connection.Response response = Jsoup.connect(url)
                    .method(Connection.Method.GET)
                    .ignoreContentType(true)
                    .cookie("ASP.NET_SessionId", jsonNode.get("cookie").asText())
                    .followRedirects(false)
                    .execute();

            return response.bodyAsBytes();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @RequestMapping(value = {"/{university}", "/{university}/{system}"}, method = RequestMethod.POST)
    public ResponseEntity getStudent(
            @PathVariable("university") String university,
            @PathVariable(required = false) String system,
            @RequestBody LoginForm loginForm) {
        return student.get(university, system, loginForm);
    }
}
