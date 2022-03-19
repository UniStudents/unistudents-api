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

public class HMUScraper {
    private final String USER_AGENT;
    private boolean connected;
    private boolean authorized;
    private String infoJSON;
    private String gradesJSON;
    private Map<String, String> cookies;
    private final Logger logger = LoggerFactory.getLogger(HMUScraper.class);

    public HMUScraper(LoginForm loginForm) {
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
        final String execution;
        final String _eventId;
        final String bearerToken;
        final String ticket;
        final String state = StringHelper.getRandomHashcode();

        //
        // Get Login Page
        //

        try {
            response = Jsoup.connect("https://auth.hmu.gr/oauth?redirect_uri=https%3A%2F%2Fstudents.hmu.gr%2Fauth%2Fcallback%2F&response_type=token&client_id=6065706863394382&scope=students&state=" + state)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate")
                    .header("Accept-Language", "en-US,en;q=0.9,el-GR;q=0.8,el;q=0.7")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Request", "1")
                    .followRedirects(true)
                    .header("User-Agent", USER_AGENT)
                    .method(Connection.Method.GET)
                    .execute();
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            connected = false;
            logger.warn("[HMU] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[HMU] Error: {}", e.getMessage(), e);
            return;
        }

        try {
            String jsonupload = "{\"redirect_uri\":\"https://students.hmu.gr/auth/callback/\",\"response_type\":\"token\",\"client_id\":\"6065706863394382\",\"scope\":\"students\",\"state\":\"" + state + "\"}";
            response = Jsoup.connect("https://auth.hmu.gr/cas/state")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Content-Type", "application/json")
                    .requestBody(jsonupload)
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Connection", "keep-alive")
                    .header("Host", "auth.hmu.gr")
                    .header("Origin", "https://auth.hmu.gr")
                    .header("Referer", "https://auth.hmu.gr/oauth2/cas-login?redirect_uri=https:%2F%2Fstudents.hmu.gr%2Fauth%2Fcallback%2F&response_type=token&client_id=6065706863394382&scope=students&state=" + state)
                    .header("Sec-Fetch-Dest", "empty")
                    .header("Sec-Fetch-Mode", "cors")
                    .header("Sec-Fetch-Site", "same-site")
                    .header("TE", "trailers")
                    .header("DNT", "1")
                    .header("User-Agent", USER_AGENT)
                    .method(Connection.Method.POST)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .followRedirects(false)
                    .execute();
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            connected = false;
            logger.warn("[HMU] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[HMU] Error: {}", e.getMessage(), e);
            return;
        }

        try {
            response = Jsoup.connect("https://cas.hmu.gr/cas/login?service=https://auth.hmu.gr/idplogin/?state=" + state)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate")
                    .header("Accept-Language", "en-US,en;q=0.9,el-GR;q=0.8,el;q=0.7")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Request", "1")
                    .followRedirects(true)
                    .header("User-Agent", USER_AGENT)
                    .method(Connection.Method.GET)
                    .execute();

            Document document = response.parse();
            execution = document.getElementsByAttributeValue("name", "execution").attr("value");
            _eventId = document.getElementsByAttributeValue("name", "_eventId").attr("value");

            if (execution == null || execution.isEmpty()) return;
            if (_eventId == null || _eventId.isEmpty()) return;
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            connected = false;
            logger.warn("[HMU] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[HMU] Error: {}", e.getMessage(), e);
            return;
        }


        //
        // Try login
        //

        try {
            response = Jsoup.connect("https://cas.hmu.gr/cas/login")
                    .data("username", username)
                    .data("password", password)
                    .data("execution", execution)
                    .data("_eventId", _eventId)
                    .data("geolocation", "")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Host", "cas.hmu.gr")
                    .header("Origin", "https://cas.hmu.gr")
                    .header("Referer", "https://cas.hmu.gr/cas/login?service=https://auth.hmu.gr/idplogin/?state=" + state)
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "same-origin")
                    .header("User-Agent", USER_AGENT)
                    .method(Connection.Method.POST)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .execute();

            String url = response.url().toString();
            ticket = url.substring(url.indexOf("ticket=") + "ticket=".length());
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            connected = false;
            logger.warn("[HMU] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[HMU] Error: {}", e.getMessage(), e);
            return;
        }

        try {
            String jsonUpload = "{\"ticket\": \"" + ticket + "\",\"state\":\"" + state + "\"}";
            response = Jsoup.connect("https://auth.hmu.gr/cas/ticket")
                    .header("Accept", "application/json, text/plain, */*")
                    .requestBody(jsonUpload)
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Content-Type", "application/json")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Connection", "keep-alive")
                    .header("Host", "auth.hmu.gr")
                    .header("Origin", "https://auth.hmu.gr")
                    .header("Referer", "https://auth.hmu.gr/oauth2/cas-logged?state=" + state + "&ticket=" + ticket)
                    .header("Sec-Fetch-Dest", "empty")
                    .header("Sec-Fetch-Mode", "cors")
                    .header("Sec-Fetch-Site", "same-site")
                    .header("TE", "trailers")
                    .header("DNT", "1")
                    .header("User-Agent", USER_AGENT)
                    .method(Connection.Method.POST)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .followRedirects(false)
                    .execute();

            Document document = response.parse();

            if (document.text().contains("invalid cas result")) {
                authorized = false;
                return;
            }

            String _document = document.toString();
            bearerToken = _document.substring(_document.indexOf("\"redirectTo\":\"https://students.hmu.gr/auth/callback/?access_token=") + 66, _document.indexOf("\"}"));
            logger.debug(bearerToken);
            if (bearerToken.isEmpty()) return;
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            connected = false;
            logger.warn("[HMU] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[HMU] Error: {}", e.getMessage(), e);
            return;
        }

        //
        // Get student's information
        //

        try {
            response = Jsoup.connect("https://universis-api.hmu.gr/api/students/me/?$expand=user,department,studyProgram,inscriptionMode,person($expand=gender)&$top=1&$skip=0&$count=false")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Authorization", "Bearer " + bearerToken)
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/json")
                    .header("Host", "universis-api.hmu.gr")
                    .header("Origin", "https://students.hmu.gr")
                    .header("Referer", "https://students.hmu.gr/")
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
            logger.warn("[HMU] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[HMU] Error: {}", e.getMessage(), e);
            return;
        }


        //
        // Get student's grades
        //

        try {
            response = Jsoup.connect("https://universis-api.hmu.gr/api/students/me/courses/?$expand=course($expand=locale),courseType($expand=locale),gradeExam($expand=instructors($expand=instructor($select=id,givenName,familyName,category,locale)))&$orderby=semester%20desc,gradeYear%20desc&$top=-1&$count=false")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Authorization", "Bearer " + bearerToken)
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/json")
                    .header("Host", "universis-api.hmu.gr")
                    .header("Origin", "https://students.hmu.gr")
                    .header("Referer", "https://students.hmu.gr/")
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
            logger.warn("[HMU] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[HMU] Error: {}", e.getMessage(), e);
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
            response = Jsoup.connect("https://universis-api.hmu.gr/api/students/me/?$expand=user,department,studyProgram,inscriptionMode,person($expand=gender)&$top=1&$skip=0&$count=false")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Authorization", "Bearer " + bearerToken)
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/json")
                    .header("Host", "universis-api.hmu.gr")
                    .header("Origin", "https://students.hmu.gr")
                    .header("Referer", "https://students.hmu.gr/")
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
            logger.warn("[HMU] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[HMU] Error: {}", e.getMessage(), e);
            return;
        }


        //
        // Get student's grades
        //

        try {
            response = Jsoup.connect("https://universis-api.hmu.gr/api/students/me/courses/?$expand=course($expand=locale),courseType($expand=locale),gradeExam($expand=instructors($expand=instructor($select=id,givenName,familyName,category,locale)))&$orderby=semester%20desc,gradeYear%20desc&$top=-1&$count=false")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Authorization", "Bearer " + bearerToken)
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/json")
                    .header("Host", "universis-api.hmu.gr")
                    .header("Origin", "https://students.hmu.gr")
                    .header("Referer", "https://students.hmu.gr/")
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
            logger.warn("[HMU] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[HMU] Error: {}", e.getMessage(), e);
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
