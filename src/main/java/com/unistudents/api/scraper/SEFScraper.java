package com.unistudents.api.scraper;

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
import java.util.Map;

public class SEFScraper {
    private final String USER_AGENT;
    private boolean connected;
    private boolean authorized;
    private Document studentInfoPage;
    private Document gradesPage;
    private Map<String, String> cookies;
    private final Logger logger = LoggerFactory.getLogger(SEFScraper.class);

    public SEFScraper(LoginForm loginForm) {
        this.connected = true;
        this.authorized = true;
        USER_AGENT = UserAgentGenerator.generate();
        getDocuments(loginForm.getUsername(), loginForm.getPassword(), loginForm.getCookies());
    }

    private void getDocuments(String username, String password, Map<String, String> cookies) {
        if (cookies == null) {
            getHtmlPages(username, password);
        } else {
            getHtmlPages(cookies);
            if (studentInfoPage == null || gradesPage == null) {
                getHtmlPages(username, password);
            }
        }
    }

    private void getHtmlPages(String username, String password) {
        username = username.trim();
        password = password.trim();

        Connection.Response response;
        Map<String, String> cookies;
        boolean authorized;

        // Get cookies
        try {
            response = Jsoup.connect("https://sef.samos.aegean.gr")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "el-GR,el;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("Connection", "keep-alive")
                    .header("DNT", "1")
                    .header("Host", "sef.samos.aegean.gr")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("User-Agent", USER_AGENT)
                    .method(Connection.Method.GET)
                    .execute();

            cookies = response.cookies();
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException connException) {
            connected = false;
            logger.warn("Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("Error: {}", e.getMessage(), e);
            return;
        }

        // Attempt to connect and get grades page
        try {
            response = Jsoup.connect("https://sef.samos.aegean.gr/authentication.php")
                    .data("username", username)
                    .data("password", password)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "el-GR,el;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("Connection", "keep-alive")
                    .header("Content-Length", "33")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("DNT", "1")
                    .header("Host", "sef.samos.aegean.gr")
                    .header("Origin", "https://sef.samos.aegean.gr")
                    .header("Referer", "https://sef.samos.aegean.gr/")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("User-Agent", USER_AGENT)
                    .method(Connection.Method.GET)
                    .cookies(cookies)
                    .execute();

            authorized = authorizationCheck(response.parse());
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException connException) {
            connected = false;
            logger.warn("Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("Error: {}", e.getMessage(), e);
            return;
        }

        // Return if not authorized
        if (!authorized) {
            this.authorized = false;
            return;
        }

        // Set grades page
        try {
            setGradesPage(response.parse());
        } catch (IOException e) {
            logger.error("Error: {}", e.getMessage(), e);
        }

        // Get info page
        try {
            response = Jsoup.connect("https://sef.samos.aegean.gr/request.php")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "el-GR,el;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("Connection", "keep-alive")
                    .header("Content-Length", "33")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("DNT", "1")
                    .header("Host", "sef.samos.aegean.gr")
                    .header("Origin", "https://sef.samos.aegean.gr")
                    .header("Referer", "https://sef.samos.aegean.gr/")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("User-Agent", USER_AGENT)
                    .method(Connection.Method.GET)
                    .cookies(cookies)
                    .execute();

            setStudentInfoPage(response.parse());
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException connException) {
            connected = false;
            logger.warn("Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("Error: {}", e.getMessage(), e);
            return;
        }

        setCookies(cookies);
    }

    private void getHtmlPages(Map<String, String> cookies) {
        Connection.Response response;
        boolean authorized;

        // Attempt to connect and get grades page
        try {
            response = Jsoup.connect("https://sef.samos.aegean.gr/authentication.php")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "el-GR,el;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("Connection", "keep-alive")
                    .header("Content-Length", "33")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("DNT", "1")
                    .header("Host", "sef.samos.aegean.gr")
                    .header("Origin", "https://sef.samos.aegean.gr")
                    .header("Referer", "https://sef.samos.aegean.gr/")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("User-Agent", USER_AGENT)
                    .method(Connection.Method.GET)
                    .cookies(cookies)
                    .execute();

            authorized = authorizationCheck(response.parse());
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException connException) {
            connected = false;
            logger.warn("Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("Error: {}", e.getMessage(), e);
            return;
        }

        if (response.statusCode() != 200) return;

        // Set grades page
        try {
            setGradesPage(response.parse());
        } catch (IOException e) {
            logger.error("Error: {}", e.getMessage(), e);
        }

        // Get info page
        try {
            response = Jsoup.connect("https://sef.samos.aegean.gr/request.php")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "el-GR,el;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("Connection", "keep-alive")
                    .header("Content-Length", "33")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("DNT", "1")
                    .header("Host", "sef.samos.aegean.gr")
                    .header("Origin", "https://sef.samos.aegean.gr")
                    .header("Referer", "https://sef.samos.aegean.gr/")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("User-Agent", USER_AGENT)
                    .method(Connection.Method.GET)
                    .cookies(cookies)
                    .execute();

            setStudentInfoPage(response.parse());
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException connException) {
            connected = false;
            logger.warn("Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("Error: {}", e.getMessage(), e);
            return;
        }
    }



    private boolean authorizationCheck(Document document) {
        String html = document.toString();

        return !html.contains("Δεν έχετε δικαίωμα πρόσβασης στο Σ.Ε.Φ. Ελέγξτε τα στοιχεία σας και προσπαθήστε ξανά..");
    }

    public Document getStudentInfoPage() {
        return studentInfoPage;
    }

    private void setStudentInfoPage(Document studentInfoPage) {
        this.studentInfoPage = studentInfoPage;
    }

    public Document getGradesPage() {
        return gradesPage;
    }

    private void setGradesPage(Document gradesPage) {
        this.gradesPage = gradesPage;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public boolean isConnected() {
        return connected;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }
}
