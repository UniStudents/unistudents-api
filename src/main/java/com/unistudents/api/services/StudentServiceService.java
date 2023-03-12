package com.unistudents.api.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unistudents.api.common.UserAgentGenerator;
import com.unistudents.api.components.LoginForm;
import gr.unistudents.services.student.StudentService;
import gr.unistudents.services.student.components.Options;
import gr.unistudents.services.student.exceptions.*;
import gr.unistudents.services.student.components.StudentResponse;
import io.sentry.Attachment;
import io.sentry.Hint;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
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
import java.util.Random;

@Service
public class StudentServiceService {

    private final Logger logger = LoggerFactory.getLogger(StudentServiceService.class);

    private String getUniForLogs(String university, String system) {
        return university + (system != null && system.trim().length() != 0 ? "." + system : "");
    }
    
    private ResponseEntity<Object> guest(String university, String system, LoginForm loginForm) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    private ResponseEntity getGuestStudent() {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = null;

        try {
            byte[] encoded = Files.readAllBytes(Paths.get("src/main/resources/guestStudent.json"));
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

    public ResponseEntity<Object> get(String university, String system, LoginForm loginForm, boolean getDocuments) {
        if (isGuest(loginForm))
            return getGuestStudent();

        Options options = new Options();
        options.university = university.toLowerCase(Locale.ROOT);
        options.system = system != null ? system.toLowerCase(Locale.ROOT) : null;
        options.username = loginForm.getUsername();
        options.password = loginForm.getPassword();
        options.cookies = loginForm.getCookies();
        options.userAgent = UserAgentGenerator.generate();

        try {
            StudentResponse response = StudentService.get(options);
            if (response.getCaptchaSystem() != null) {
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if(!getDocuments)
                response.documents = null;
            
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

            if (e.document != null && e.document.length() > 0) {
                Attachment attachment = new Attachment(e.document.getBytes(StandardCharsets.UTF_8), "document.txt");
                Hint hint = new Hint();
                hint.addAttachment(attachment);
                Sentry.captureException(th, hint, scope -> {
                    scope.setTag("university", university);
                    scope.setTag("exception-class", "ParserException");
                    scope.setLevel(SentryLevel.ERROR);
                });
            } else {
                Sentry.captureException(th, scope -> {
                    scope.setTag("university", university);
                    scope.setTag("exception-class", "ParserException");
                    scope.setLevel(SentryLevel.ERROR);
                });
            }

            // Send to analytics
            logger.error("[" + getUniForLogs(university, system) + "] Parser error: " + e.getMessage(), th);
            
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
            logger.error("[" + getUniForLogs(university, system) + "] Scraper error: " + e.getMessage(), th);
            
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
            logger.error("[" + getUniForLogs(university, system) + "] General error: " + e.getMessage(), e);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public byte[] getCaptchaImage(String university, JsonNode jsonNode) throws IOException {
        switch (university.toUpperCase()) {
            case "UPATRAS-PROGRESS":
            case "UPATRAS":
                return captchaUPATRAS(jsonNode);
            case "UPATRAS-TEIWEST":
            case "UOP-TEIWEST":
                return captchaTEIWEST(jsonNode);
        }
        return null;
    }

    private byte[] captchaTEIWEST(JsonNode jsonNode) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).build();

        String d = String.valueOf(new Random().nextInt(999999999) + 1000000000);
        Request request = new Request.Builder().url(jsonNode.get("cookies").get("captchaImageUrl").asText() + "&d=" + d)
                .header("Cookie", jsonNode.get("cookies").get("cookieStr").asText())
                .build();

        Response response = client.newCall(request).execute();

        return response.body().bytes();
    }

    private byte[] captchaUPATRAS(JsonNode jsonNode) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).build();

        Request request = new Request.Builder().url("https://matrix.upatras.gr/sap/bc/webdynpro/SAP/" + jsonNode.get("cookies").get("url2").asText())
                .post(new FormBody.Builder()
                        .add("sap-charset", "utf-8")
                        .add("sap-wd-secure-id", jsonNode.get("cookies").get("sap_wd_secure_id").asText())
                        .add("SAPEVENTQUEUE", "Button_Press~E002Id~E004WD45~E003~E002ResponseData~E004delta~E005ClientAction~E004submit~E003~E002~E003~E001Form_Request~E002Id~E004sap.client.SsrClient.form~E005Async~E004false~E005FocusInfo~E004~0040~007B~0022sFocussedId~0022~003A~0022WD45~0022~007D~E005Hash~E004~E005DomChanged~E004false~E005IsDirty~E004false~E003~E002ResponseData~E004delta~E003~E002~E003")
                        .build())
                .header("Cookie", jsonNode.get("cookies").get("cookieStr").asText())
                .build();

        Response response = client.newCall(request).execute();

        String document = response.body().string();

        String i1 = "&#x5c;x2f";
        String imageStr = document.split(".png'}\"")[0];
        String imageId = imageStr.substring(imageStr.lastIndexOf(i1) + i1.length());

        request = new Request.Builder().url("https://matrix.upatras.gr/sap/bc/webdynpro/sap/zups_piq_st_acad_work_ov/" + imageId + ".png")
                .header("Cookie", jsonNode.get("cookies").get("cookieStr").asText())
                .build();

        response = client.newCall(request).execute();

        return response.body().bytes();
    }
}
