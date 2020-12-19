package com.unistudents.api.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unistudents.api.common.JWTUtils;
import com.unistudents.api.common.UserAgentGenerator;
import com.unistudents.api.model.LoginForm;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class NTUAScraper {
    private final String USER_AGENT;
    private boolean connected;
    private boolean authorized;
    private String studentInfoAndGradesPage;
    private Map<String, String> cookies;
    private final Logger logger = LoggerFactory.getLogger(NTUAScraper.class);

    public NTUAScraper(LoginForm loginForm) {
        this.connected = true;
        this.authorized = true;
        USER_AGENT = UserAgentGenerator.generate();
        this.getDocuments(loginForm.getUsername(), loginForm.getPassword(), loginForm.getCookies());
    }

    private void getDocuments(String username, String password, Map<String, String> cookies) {
        if (cookies == null) {
            getHtmlPages(username, password);
        } else {
            getHtmlPages(cookies);
            if (studentInfoAndGradesPage == null) {
                getHtmlPages(username, password);
            }
        }
    }

    private void getHtmlPages(String username, String password) {
        username = username.trim();
        password = password.trim();
        Connection.Response response;

        //
        // Make sure server is not down
        //

        try {
            response = Jsoup.connect("http://www.central.ntua.gr/")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate")
                    .header("Accept-Language", "en-US,en;q=0.9,el-GR;q=0.8,el;q=0.7")
                    .header("Connection", "keep-alive")
                    .header("Host", "www.central.ntua.gr")
                    .header("Upgrade-Insecure-Request", "1")
                    .header("User-Agent", USER_AGENT)
                    .method(Connection.Method.GET)
                    .execute();
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException connException) {
            authorized = false;
            logger.warn("[NTUA] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[NTUA] Error: {}", e.getMessage(), e);
            return;
        }

        //
        // Try login
        //

        try {
            response = Jsoup.connect("https://backend.central.ntua.gr/user/login")
                    .data("username", username)
                    .data("password", password)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Connection", "keep-alive")
                    .header("Host", "backend.central.ntua.gr")
                    .header("Origin", "https://my.central.ntua.gr")
                    .header("Referer", "https://my.central.ntua.gr/")
                    .header("Sec-Fetch-Dest", "empty")
                    .header("Sec-Fetch-Mode", "cors")
                    .header("Sec-Fetch-Site", "same-site")
                    .header("User-Agent", USER_AGENT)
                    .method(Connection.Method.POST)
                    .ignoreContentType(true)
                    .execute();
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException connException) {
            authorized = false;
            logger.warn("[NTUA] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[NTUA] Error: {}", e.getMessage(), e);
            return;
        }

        if (response.statusCode() != 200) {
            authorized = false;
            return;
        }

        Document document;
        String token;
        String department;
        String category;
        try {
            document = response.parse();
            if (document == null) return;

            // get token
            token = document.text();
            JsonNode node = new ObjectMapper().readTree(token);
            token = node.get("token").asText();

            // decode token
            String[] values = JWTUtils.decoded(token);
            if (values == null) return;

            department = values[0];
            category = values[1];
        } catch (IOException e) {
            logger.error("[NTUA] Error: {}", e.getMessage(), e);
            return;
        }

        if (token == null) return;

        //
        // Get student's information
        //

        try {
            response = Jsoup.connect("https://backend.central.ntua.gr/profile/get-profile-by-username-or-id")
                    .data("student", username)
                    .data("category", category)
                    .data("department", department)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Authorization", "Bearer " + token)
                    .header("Connection", "keep-alive")
                    .header("Host", "backend.central.ntua.gr")
                    .header("Origin", "https://my.central.ntua.gr")
                    .header("Referer", "https://my.central.ntua.gr/")
                    .header("Sec-Fetch-Dest", "empty")
                    .header("Sec-Fetch-Mode", "cors")
                    .header("Sec-Fetch-Site", "same-site")
                    .header("User-Agent", USER_AGENT)
                    .method(Connection.Method.POST)
                    .ignoreContentType(true)
                    .execute();
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException connException) {
            connected = false;
            logger.warn("[NTUA] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[NTUA] Error: {}", e.getMessage(), e);
            return;
        }

        try {
            document = response.parse();
            if (document.outerHtml().contains("Λάθος αριθμός μητρώου ή username")) return;
            setStudentInfoAndGradesPage(document.text());
            Map<String, String> cookies = new HashMap<>();
            cookies.put("username", username);
            cookies.put("token", token);
            cookies.put("department", department);
            cookies.put("category", category);
            setCookies(cookies);
        } catch (IOException e) {
            logger.error("[NTUA] Error: {}", e.getMessage(), e);
        }
    }

    private void getHtmlPages(Map<String, String> cookies) {
        Connection.Response response;
        Document document;

        String username = cookies.get("username");
        String token = cookies.get("token");
        String department = cookies.get("department");
        String category = cookies.get("category");
        if (username == null || token == null || department == null || category == null) return;

        try {
            response = Jsoup.connect("https://backend.central.ntua.gr/profile/get-profile-by-username-or-id")
                    .data("student", username)
                    .data("category", category)
                    .data("department", department)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Authorization", "Bearer " + token)
                    .header("Connection", "keep-alive")
                    .header("Host", "backend.central.ntua.gr")
                    .header("Origin", "https://my.central.ntua.gr")
                    .header("Referer", "https://my.central.ntua.gr/")
                    .header("Sec-Fetch-Dest", "empty")
                    .header("Sec-Fetch-Mode", "cors")
                    .header("Sec-Fetch-Site", "same-site")
                    .header("User-Agent", USER_AGENT)
                    .method(Connection.Method.POST)
                    .ignoreContentType(true)
                    .execute();
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException connException) {
            logger.warn("[NTUA] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[NTUA] Error: {}", e.getMessage(), e);
            return;
        }

        if (response.statusCode() != 200) return;

        try {
            document = response.parse();
            if (document.outerHtml().contains("Λάθος αριθμός μητρώου ή username")) return;
            setStudentInfoAndGradesPage(document.text());
            setCookies(cookies);
        } catch (IOException e) {
            logger.error("[NTUA] Error: {}", e.getMessage(), e);
        }
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

    public String getStudentInfoAndGradesPage() {
        return studentInfoAndGradesPage;
    }

    public void setStudentInfoAndGradesPage(String studentInfoAndGradesPage) {
        this.studentInfoAndGradesPage = studentInfoAndGradesPage;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }
}
