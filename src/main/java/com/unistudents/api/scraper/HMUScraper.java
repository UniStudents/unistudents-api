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
        ***REMOVED***
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
        final String bearerToken;
        final String state = StringHelper.getRandomHashcode();

        //
        // Get Login Page
        //

        try {
            response = Jsoup.connect("https://auth.hmu.gr/oauth2/login?redirect_uri=https://students.hmu.gr/auth/callback/&response_type=token&client_id=6065706863394382&scope=students&state=" + state)
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

        //
        // Authorize
        //

        try {
            String jsonUpload = "{\"username\":\"" + username + "\", \"password\": \"" + password + "\", \"client_id\": \"6065706863394382\", \"state\": \"" + state + "\", \"response_type\": \"token\", \"redirect_uri\": \"https://students.hmu.gr/auth/callback/\", \"grant_type\": \"authorization_code\", \"scope\": \"students\", \"json\":true}";
            response = Jsoup.connect("https://auth.hmu.gr/auth/authorize")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Content-Type", "application/json")
                    .requestBody(jsonUpload)
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Connection", "keep-alive")
                    .header("Host", "auth.hmu.gr")
                    .header("Origin", "https://auth.hmu.gr")
                    .header("Referer", "https://auth.hmu.gr/oauth2/login?redirect_uri=https:%2F%2Fstudents.hmu.gr%2Fauth%2Fcallback%2F&response_type=token&client_id=6065706863394382&scope=students&state=" + state)
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
            String _document = document.toString();

            if (!_document.contains("location")) {
                this.authorized = false;
                return;
            }

            bearerToken = _document.substring(
                    _document.indexOf("access_token=") + "access_token=".length(),
                    _document.indexOf("&amp;state"));
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
