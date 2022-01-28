package com.unistudents.api.scraper;

import com.unistudents.api.common.StringHelper;
import com.unistudents.api.common.UserAgentGenerator;
import com.unistudents.api.model.LoginForm;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class AEGEANScraper {
    private final String USER_AGENT;
    private boolean connected;
    private boolean authorized;
    private String infoJSON;
    private String gradesJSON;
    private Map<String, String> cookies;
    private final Logger logger = LoggerFactory.getLogger(AEGEANScraper.class);

    public AEGEANScraper(LoginForm loginForm) {
        this.connected = true;
        this.authorized = true;
        this.USER_AGENT = UserAgentGenerator.generate();
        this.getDocuments(loginForm.getUsername(), loginForm.getPassword(), loginForm.getCookies());
    }

    private void getDocuments(String username, String password, Map<String, String> cookies) {
        if (cookies == null) {
            getJSONFiles(username, password);
        } else {
            getJSONFiles(cookies);
            if (infoJSON == null || gradesJSON == null) {
                getJSONFiles(username, password);
            }
        }
    }

    private void getJSONFiles(String username, String password) {
        username = username.trim();
        password = password.trim();
        Connection.Response response;
        String formURL;
        final String SAMLRequest;
        final String SAMLResponse;
        String RelayState;
        final String lt;
        final String execution;
        final String _eventId;
        final String submitForm;
        final String bearerToken;
        final String state = StringHelper.getRandomHashcode();
        HashMap<String, String> cookiesObj = new HashMap<>();


        //
        // Get Login Page
        //

        try {
            response = Jsoup.connect("https://uni-oauth.aegean.gr/auth/realms/universis/protocol/openid-connect/auth?redirect_uri=https%3A%2F%2Funi-student.aegean.gr%2Fauth%2Fcallback%2Findex.html&response_type=token&client_id=universis-student&scope=students&state=" + state)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate")
                    .header("Accept-Language", "en-US,en;q=0.9,el-GR;q=0.8,el;q=0.7")
                    .header("Connection", "keep-alive")
                    .header("Host", "uni-oauth.aegean.gr")
                    .header("Referer", "https://studentweb.aegean.gr/")
                    .header("Upgrade-Insecure-Request", "1")
                    .header("User-Agent", USER_AGENT)
                    .method(Connection.Method.GET)
                    .execute();

            Document document = response.parse();
            formURL = document.select("form").attr("action");
            SAMLRequest = document.getElementsByAttributeValue("name", "SAMLRequest").attr("value");
            RelayState = document.getElementsByAttributeValue("name", "RelayState").attr("value");

            if (formURL == null || formURL.isEmpty()) return;
            if (SAMLRequest == null || SAMLRequest.isEmpty()) return;
            if (RelayState == null || RelayState.isEmpty()) return;
            cookiesObj.putAll(response.cookies());
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            connected = false;
            logger.warn("[AEGEAN.UNIVERSIS] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[AEGEAN.UNIVERSIS] Error: {}", e.getMessage(), e);
            return;
        }



        //
        // Get AuthState
        //

        try {
            response = Jsoup.connect(formURL)
                    .data("SAMLRequest", SAMLRequest)
                    .data("RelayState", RelayState)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Host", "idp.aegean.gr")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "same-origin")
                    .header("User-Agent", USER_AGENT)
                    .method(Connection.Method.POST)
                    .execute();

            Document document = response.parse();
            formURL = document.getElementById("fm1").attr("action");
            lt = document.getElementsByAttributeValue("name", "lt").attr("value");
            execution = document.getElementsByAttributeValue("name", "execution").attr("value");
            _eventId = document.getElementsByAttributeValue("name", "_eventId").attr("value");
            submitForm = document.getElementsByAttributeValue("name", "submitForm").attr("value");

            if (formURL == null || formURL.isEmpty()) return;
            if (lt == null || lt.isEmpty()) return;
            if (execution == null || execution.isEmpty()) return;
            if (_eventId == null || _eventId.isEmpty()) return;
            if (submitForm == null || submitForm.isEmpty()) return;
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            connected = false;
            logger.warn("[AEGEAN.UNIVERSIS] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[AEGEAN.UNIVERSIS] Error: {}", e.getMessage(), e);
            return;
        }



        //
        // Try login
        //

        try {
            response = Jsoup.connect("https://sso.aegean.gr" + formURL)
                    .data("username", username)
                    .data("password", password)
                    .data("lt", lt)
                    .data("execution", execution)
                    .data("_eventId", _eventId)
                    .data("submitForm", submitForm)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Host", "sso.aegean.gr")
                    .header("Origin", "https://sso.aegean.gr")
                    .header("Referer", "https://sso.aegean.gr/login?service=https%3A%2F%2Fidp.aegean.gr%2Fcasauth%2Ffacade%2Fnorenew%3Fidp%3Dhttps%3A%2F%2Fidp.aegean.gr%2Fidp%2FexternalAuthnCallback")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "same-origin")
                    .header("User-Agent", USER_AGENT)
                    .method(Connection.Method.POST)
                    .cookies(response.cookies())
                    .execute();

            Document document = response.parse();
            String url = response.url().toString();
            System.out.println(document);
            System.out.println("Final url: " + url);
            System.out.println(response.cookies());

            if (document.text().contains("το όνομα χρήστη ή ο κωδικός πρόσβασης ήταν λάθος")) {
                authorized = false;
                return;
            }

            formURL = document.select("form").attr("action");
            SAMLResponse = document.getElementsByAttributeValue("name", "SAMLResponse").attr("value");
            RelayState = document.getElementsByAttributeValue("name", "RelayState").attr("value");

            if (formURL == null || formURL.isEmpty()) return;
            if (SAMLResponse.isEmpty()) return;
            if (RelayState.isEmpty()) return;
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            connected = false;
            logger.warn("[AEGEAN.UNIVERSIS] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[AEGEAN.UNIVERSIS] Error: {}", e.getMessage(), e);
            return;
        }


        //
        // Proceed
        //

        try {
            response = Jsoup.connect(formURL)
                    .data("SAMLResponse", SAMLResponse)
                    .data("RelayState", RelayState)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Host", "uni-oauth.aegean.gr")
                    .header("Origin", "https://idp.aegean.gr")
                    .header("Referer", "https://idp.aegean.gr/")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "same-origin")
                    .header("User-Agent", USER_AGENT)
                    .method(Connection.Method.POST)
                    .cookies(cookiesObj)
                    .ignoreHttpErrors(true)
                    .followRedirects(false)
                    .execute();

            String url = response.header("location");
            if (!url.contains("access_token=") || !url.contains("&token_type")) {
                return;
            }

            bearerToken = url.substring(
                    url.indexOf("access_token=") + "access_token=".length(),
                    url.indexOf("&token_type"));

            if (bearerToken.isEmpty()) return;
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            connected = false;
            logger.warn("[AEGEAN.UNIVERSIS] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[AEGEAN.UNIVERSIS] Error: {}", e.getMessage(), e);
            return;
        }


        //
        // Get student's information
        //

        try {
            response = Jsoup.connect("https://uni-extapi.aegean.gr/api/students/me/?$expand=user,department,studyProgram,inscriptionMode,person($expand=gender)&$top=1&$skip=0&$count=false")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Authorization", "Bearer " + bearerToken)
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/json")
                    .header("Host", "uni-extapi.aegean.gr")
                    .header("Origin", "https://uni-student.aegean.gr")
                    .header("Referer", "https://uni-student.aegean.gr/")
                    .header("Sec-Fetch-Dest", "empty")
                    .header("Sec-Fetch-Mode", "cors")
                    .header("Sec-Fetch-Site", "same-site")
                    .header("User-Agent", USER_AGENT)
                    .method(Connection.Method.GET)
                    .ignoreContentType(true)
                    .execute();

            Document document = response.parse();
            setInfoJSON(document.text());
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            connected = false;
            logger.warn("[AEGEAN.UNIVERSIS] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[AEGEAN.UNIVERSIS] Error: {}", e.getMessage(), e);
            return;
        }


        //
        // Get student's grades
        //

        try {
            response = Jsoup.connect("https://uni-extapi.aegean.gr/api/students/me/courses/?$expand=course($expand=locale),courseType($expand=locale),gradeExam($expand=instructors($expand=instructor($select=id,givenName,familyName,category,locale)))&$orderby=semester%20desc,gradeYear%20desc&$top=-1&$count=false")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Authorization", "Bearer " + bearerToken)
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/json")
                    .header("Host", "uni-extapi.aegean.gr")
                    .header("Origin", "https://uni-student.aegean.gr")
                    .header("Referer", "https://uni-student.aegean.gr/")
                    .header("Sec-Fetch-Dest", "empty")
                    .header("Sec-Fetch-Mode", "cors")
                    .header("Sec-Fetch-Site", "same-site")
                    .header("User-Agent", USER_AGENT)
                    .method(Connection.Method.GET)
                    .ignoreContentType(true)
                    .execute();

            Document document = response.parse();
            setGradesJSON(document.text());
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            connected = false;
            logger.warn("[AEGEAN.UNIVERSIS] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[AEGEAN.UNIVERSIS] Error: {}", e.getMessage(), e);
            return;
        }

        Map<String, String> cookies = new HashMap<String, String>() {{
            put("bearerToken", bearerToken);
        }};
        setCookies(cookies);
    }

    private void getJSONFiles(Map<String, String> cookies) {
        Connection.Response response;
        String bearerToken = cookies.get("bearerToken");
        if (bearerToken == null || bearerToken.isEmpty()) return;

        //
        // Get student's information
        //

        try {
            response = Jsoup.connect("https://uni-extapi.aegean.gr/api/students/me/?$expand=user,department,studyProgram,inscriptionMode,person($expand=gender)&$top=1&$skip=0&$count=false")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Authorization", "Bearer " + bearerToken)
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/json")
                    .header("Host", "uni-extapi.aegean.gr")
                    .header("Origin", "https://uni-student.aegean.gr")
                    .header("Referer", "https://uni-student.aegean.gr/")
                    .header("Sec-Fetch-Dest", "empty")
                    .header("Sec-Fetch-Mode", "cors")
                    .header("Sec-Fetch-Site", "same-site")
                    .header("User-Agent", USER_AGENT)
                    .method(Connection.Method.GET)
                    .ignoreContentType(true)
                    .execute();

            Document document = response.parse();
            setInfoJSON(document.text());
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            logger.warn("[AEGEAN.UNIVERSIS] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[AEGEAN.UNIVERSIS] Error: {}", e.getMessage(), e);
            return;
        }


        //
        // Get student's grades
        //

        try {
            response = Jsoup.connect("https://uni-extapi.aegean.gr/api/students/me/courses/?$expand=course($expand=locale),courseType($expand=locale),gradeExam($expand=instructors($expand=instructor($select=id,givenName,familyName,category,locale)))&$orderby=semester%20desc,gradeYear%20desc&$top=-1&$count=false")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Authorization", "Bearer " + bearerToken)
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/json")
                    .header("Host", "uni-extapi.aegean.gr")
                    .header("Origin", "https://uni-student.aegean.gr")
                    .header("Referer", "https://uni-student.aegean.gr/")
                    .header("Sec-Fetch-Dest", "empty")
                    .header("Sec-Fetch-Mode", "cors")
                    .header("Sec-Fetch-Site", "same-site")
                    .header("User-Agent", USER_AGENT)
                    .method(Connection.Method.GET)
                    .ignoreContentType(true)
                    .execute();

            Document document = response.parse();
            setGradesJSON(document.text());
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            logger.warn("[AEGEAN.UNIVERSIS] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[AEGEAN.UNIVERSIS] Error: {}", e.getMessage(), e);
            return;
        }

        setCookies(cookies);
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public String getInfoJSON() {
        return infoJSON;
    }

    public void setInfoJSON(String infoJSON) {
        this.infoJSON = infoJSON;
    }

    public String getGradesJSON() {
        return gradesJSON;
    }

    public void setGradesJSON(String gradesJSON) {
        this.gradesJSON = gradesJSON;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }
}
