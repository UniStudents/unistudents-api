package com.unistudents.api.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.unistudents.api.components.LoginForm;
import com.unistudents.api.services.StudentServiceService;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

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
            OkHttpClient client = new OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).build();

            Request request = new Request.Builder().url(url)
                    .header("Cookie", "ASP.NET_SessionId=" + jsonNode.get("cookie").asText())
                    .build();

            Response response = client.newCall(request).execute();

            return response.body().bytes();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @CrossOrigin
    @RequestMapping(value = {"/image/{university}"}, produces = MediaType.IMAGE_JPEG_VALUE, method = RequestMethod.POST)
    public @ResponseBody byte[] getCaptchaImage(
            @PathVariable("university") String university,
            @RequestBody JsonNode jsonNode) throws IOException {
        return this.student.getCaptchaImage(university, jsonNode);
    }

    @RequestMapping(value = {"/{university}", "/{university}/{system}"}, method = RequestMethod.POST)
    public ResponseEntity getStudent(
            @PathVariable("university") String university,
            @PathVariable(required = false) String system,
            @RequestBody LoginForm loginForm) {
        return student.get(university, system, loginForm);
    }
}
