package com.unistudents.api.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unistudents.api.common.UserAgentGenerator;
import com.unistudents.api.components.LoginForm;
import gr.unistudents.services.elearning.ELearningService;
import gr.unistudents.services.elearning.exceptions.NotAuthorizedException;
import gr.unistudents.services.elearning.components.Options;
import gr.unistudents.services.elearning.exceptions.NotReachableException;
import gr.unistudents.services.elearning.exceptions.ParserException;
import gr.unistudents.services.elearning.exceptions.ScraperException;
import gr.unistudents.services.elearning.models.ELearningResponse;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

@Service
public class ELearningServiceService {

    private final Logger logger = LoggerFactory.getLogger(ELearningServiceService.class);

    private ResponseEntity getGuestElearning() {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = null;

        try {
            byte[] encoded = Files.readAllBytes(Paths.get("src/main/resources/guestElearning.json"));
            String jsonFile = new String(encoded, StandardCharsets.UTF_8);

            json = mapper.readTree(jsonFile);
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

    public ResponseEntity<Object> get(String university, LoginForm loginForm) {
        if (isGuest(loginForm))
            return getGuestElearning();

        Options options = new Options();
        options.university = university.toLowerCase(Locale.ROOT);
        options.username = loginForm.getUsername();
        options.password = loginForm.getPassword();
        options.cookies = loginForm.getCookies();
        options.userAgent = UserAgentGenerator.generate();

        try {
            ELearningService result = new ELearningService(options);
            ELearningResponse response = result.get();
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (NotAuthorizedException e) {
            // Get exception
            Throwable th = e;
            if(e.exception != null)
                th = e.exception;

            // Print stack trace
            th.printStackTrace();

            Sentry.captureException(th, scope -> {
                scope.setTag("university", university);
                scope.setTag("exception-class", "NotAuthorizedException");
                scope.setLevel(SentryLevel.WARNING);
            });

            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        } catch (NotReachableException e) {
            // Get exception
            Throwable th = e;
            if(e.exception != null)
                th = e.exception;

            // Print stack trace
            th.printStackTrace();

            // Send to analytics
            Sentry.captureException(th, scope -> {
                scope.setTag("university", university);
                scope.setTag("exception-class", "NotReachableException");
                scope.setLevel(SentryLevel.WARNING);
            });
            logger.warn("[" + university + "] Not reachable: " + e.getMessage(), th);

            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        } catch (ParserException e) {
            // Get exception
            Throwable th = e;
            if(e.exception != null)
                th = e.exception;

            // Print stack trace
            th.printStackTrace();

            // Send to analytics
            Sentry.captureException(th, scope -> {
                scope.setTag("university", university);
                scope.setTag("exception-class", "ParserException");
                scope.setLevel(SentryLevel.ERROR);
            });
            logger.error("[" + university + "] Elearning Parser error: " + e.getMessage(), th);
            
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ScraperException e) {
            // Get exception
            Throwable th = e;
            if(e.exception != null)
                th = e.exception;

            // Print stack trace
            th.printStackTrace();

            // Send to analytics
            Sentry.captureException(th, scope -> {
                scope.setTag("university", university);
                scope.setTag("exception-class", "ScraperException");
                scope.setLevel(SentryLevel.ERROR);
            });
            logger.error("[" + university + "] Elearning Scraper error: " + e.getMessage(), th);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            // Print stack trace
            e.printStackTrace();

            // Send to analytics
            Sentry.captureException(e, scope -> {
                scope.setTag("university", university);
                scope.setTag("exception-class", "Exception");
                scope.setLevel(SentryLevel.ERROR);
            });
            logger.error("[" + university + "] General error: " + e.getMessage(), e);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}