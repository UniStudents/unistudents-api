package com.unistudents.api.web;

import com.unistudents.api.model.LoginForm;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class InfoController {

    // autowire info service

//    @PostMapping("/info")
//    public ResponseEntity<Info> getInfo(@RequestBody LoginForm loginForm) {
//
//        // get info object from injected service
//
//        // return object
//
//        return null;
//    }
}
