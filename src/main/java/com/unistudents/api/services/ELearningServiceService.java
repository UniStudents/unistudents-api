package com.unistudents.api.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unistudents.api.common.Logger;
import com.unistudents.api.common.UserAgentGenerator;
import com.unistudents.api.components.LoginForm;
import gr.unistudents.services.elearning.ELearningService;
import gr.unistudents.services.elearning.components.Options;
import gr.unistudents.services.elearning.exceptions.NotAuthorizedException;
import gr.unistudents.services.elearning.exceptions.NotReachableException;
import gr.unistudents.services.elearning.exceptions.ParserException;
import gr.unistudents.services.elearning.exceptions.ScraperException;
import gr.unistudents.services.elearning.models.ELearningResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

@Service
public class ELearningServiceService {

    private ResponseEntity getGuestElearning() {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json;

        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("guestElearning.json");
            json = mapper.readTree(inputStream);
            return ResponseEntity.ok(json);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private boolean isGuest(LoginForm loginForm) {
        String guestUsername = System.getenv("GUEST_USERNAME");
        String guestPassword = System.getenv("GUEST_PASSWORD");
        return loginForm.getUsername().equals(guestUsername) && loginForm.getPassword().equals(guestPassword);
    }

    private gr.unistudents.services.student.components.Options convertToStudentOptions(Options options) {
        gr.unistudents.services.student.components.Options options1 = new gr.unistudents.services.student.components.Options();
        options1.university = options.university;
        options1.username = options.username;
        options1.userAgent = options.userAgent;

        return options1;
    }

    public ResponseEntity<Object> get(String university, LoginForm loginForm) {
        if (isGuest(loginForm))
            return getGuestElearning();

        Options options = new Options();
        options.university = university.toLowerCase(Locale.ROOT);
        options.username = loginForm.getUsername();
        options.password = loginForm.getPassword();
        options.cookies = loginForm.getCookies();
        options.userAgent = UserAgentGenerator.generate();

        long timestamp = System.currentTimeMillis();

        try {
            ELearningService result = new ELearningService(options);
            ELearningResponse response = result.get();

            Logger.log(Logger.Type.ELEARNING, convertToStudentOptions(options), null, null, timestamp);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (NotAuthorizedException e) {
            // Get exception
            Throwable th = e;
            if (e.exception != null)
                th = e.exception;

            // Print stack trace
            th.printStackTrace();

            Logger.log(Logger.Type.ELEARNING, convertToStudentOptions(options), e, e.exception, timestamp);
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        } catch (NotReachableException e) {
            // Get exception
            Throwable th = e;
            if (e.exception != null)
                th = e.exception;

            // Print stack trace
            th.printStackTrace();

            Logger.log(Logger.Type.ELEARNING, convertToStudentOptions(options), e, e.exception, timestamp);
            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        } catch (ParserException e) {
            // Get exception
            Throwable th = e;
            if (e.exception != null)
                th = e.exception;

            // Print stack trace
            th.printStackTrace();

            Logger.log(Logger.Type.ELEARNING, convertToStudentOptions(options), e, e.exception, timestamp);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ScraperException e) {
            // Get exception
            Throwable th = e;
            if (e.exception != null)
                th = e.exception;

            // Print stack trace
            th.printStackTrace();

            Logger.log(Logger.Type.ELEARNING, convertToStudentOptions(options), e, e.exception, timestamp);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            // Print stack trace
            e.printStackTrace();

            Logger.log(Logger.Type.ELEARNING, convertToStudentOptions(options), e, null, timestamp);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}