package com.unistudents.api.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unistudents.api.common.Logger;
import com.unistudents.api.common.UserAgentGenerator;
import com.unistudents.api.components.LoginForm;
import gr.unistudents.services.student.StudentService;
import gr.unistudents.services.student.components.Options;
import gr.unistudents.services.student.components.StudentResponse;
import gr.unistudents.services.student.exceptions.NotAuthorizedException;
import gr.unistudents.services.student.exceptions.NotReachableException;
import gr.unistudents.services.student.exceptions.ParserException;
import gr.unistudents.services.student.exceptions.ScraperException;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

@Service
public class StudentServiceService {

    private ResponseEntity getProfile(LoginForm loginForm) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json;

        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("guests/" + loginForm.getUsername() + ".json");
            json = mapper.readTree(inputStream);
            return ResponseEntity.ok(json);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private boolean isGuest(LoginForm loginForm) {
        String credentialsJson = System.getenv("GUEST_CREDENTIALS");
        if (credentialsJson == null || credentialsJson.isEmpty()) {
            return false;
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, String>> credentials = objectMapper.readValue(credentialsJson, new TypeReference<>() {
            });

            return credentials.stream().anyMatch(cred ->
                    loginForm.getUsername().equals(cred.get("username")) && loginForm.getPassword().equals(cred.get("password"))
            );
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String migrateUniversityName(String universityName) {
        if (!universityName.endsWith(".gr")) {
            return universityName + ".gr";
        } else {
            return universityName;
        }
    }

    public ResponseEntity<Object> get(String university, String system, LoginForm loginForm, boolean getDocuments) {
        if (isGuest(loginForm))
            return getProfile(loginForm);

        Options options = new Options();
        options.university = migrateUniversityName(university).toLowerCase(Locale.ROOT);
        options.system = system != null ? system.toLowerCase(Locale.ROOT) : null;
        options.username = loginForm.getUsername();
        options.password = loginForm.getPassword();
        options.cookies = loginForm.getCookies();
        options.userAgent = UserAgentGenerator.generate();

        long timestamp = System.currentTimeMillis();

        try {
            StudentResponse response = StudentService.get(options);
            if (response.getCaptchaSystem() != null) {
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if (!getDocuments)
                response.documents = null;

            Logger.log(Logger.Type.STUDENT, options, null, null, timestamp);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (NotAuthorizedException e) {
            // Get exception
            Throwable th = e;
            if (e.exception != null)
                th = e.exception;

            // Print stack trace
            th.printStackTrace();
            Logger.log(Logger.Type.STUDENT, options, e, e.exception, timestamp);

            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        } catch (NotReachableException e) {
            // Get exception
            Throwable th = e;
            if (e.exception != null)
                th = e.exception;

            // Print stack trace
            th.printStackTrace();
            Logger.log(Logger.Type.STUDENT, options, e, e.exception, timestamp);

            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        } catch (ParserException e) {
            // Get exception
            Throwable th = e;
            if (e.exception != null)
                th = e.exception;

            // Print stack trace
            th.printStackTrace();
            Logger.log(Logger.Type.STUDENT, options, e, e.exception, timestamp);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ScraperException e) {
            // Get exception
            Throwable th = e;
            if (e.exception != null)
                th = e.exception;

            // Print stack trace
            th.printStackTrace();
            Logger.log(Logger.Type.STUDENT, options, e, e.exception, timestamp);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            // Print stack trace
            e.printStackTrace();
            Logger.log(Logger.Type.STUDENT, options, e, null, timestamp);

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
