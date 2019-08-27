package com.unistudents.api.web;

import com.unistudents.api.model.Info;
import com.unistudents.api.model.LoginForm;
import com.unistudents.api.service.InfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class InfoController {

    @Autowired
    private InfoService infoService;

    @PostMapping("/info")
    public ResponseEntity<Info> getInfo(@RequestBody LoginForm loginForm) {

        // get info object from injected service
        Info info = infoService.getInfo(loginForm.getUsername(), loginForm.getPassword());

        if (info == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        else {
            return new ResponseEntity<>(info, HttpStatus.OK);
        }
    }
}
