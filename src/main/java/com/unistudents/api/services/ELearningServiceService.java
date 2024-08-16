package com.unistudents.api.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unistudents.api.common.UserAgentGenerator;
import com.unistudents.api.components.LoginForm;
import gr.unistudents.services.elearning.ELearningService;
import gr.unistudents.services.elearning.components.Options;
import gr.unistudents.services.elearning.models.ELearningResponse;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

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
        JsonNode json;

        try {
            byte[] encoded = Files.readAllBytes(Paths.get("src/main/resources/guestElearning.json"));
            String jsonFile = new String(encoded, StandardCharsets.UTF_8);
            json = mapper.readTree(jsonFile);
            return ResponseEntity.ok(json);
        } catch (IOException e) {
            System.out.println("Error reading guest elearning file");
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
        if (isGuest(loginForm)) return getGuestElearning();

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
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorToAnalytics(e, university);
            return respondToException(e, university);
        }
    }

    private ResponseEntity<Object> respondToException(Exception e, String university) {
        switch (e.getClass().getName()) {

            case "NotAuthorizedException":
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

            case "NotReachableException":
                logger.warn("[" + university + "] Not reachable: " + e.getMessage(), e);
                return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);

            case "ParserException":
                logger.error("[" + university + "] Parser error: " + e.getMessage(), e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);

            case "ScraperException":
                logger.error("[" + university + "] Scraper error: " + e.getMessage(), e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);

            default:
                logger.error("General error: " + e.getMessage(), e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void sendErrorToAnalytics(Exception e, String university) {
        Sentry.captureException(e, scope -> {
            scope.setTag("university", university);
            scope.setTag("exception-class", e.getClass().getName());
            scope.setLevel(SentryLevel.ERROR);
        });
    }
}