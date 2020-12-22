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
    private final Logger logger = LoggerFactory.getLogger(SEFScraper.class);

    public SEFScraper(LoginForm loginForm) {
        this.connected = true;
        this.authorized = true;
        USER_AGENT = UserAgentGenerator.generate();
        getDocuments(loginForm.getUsername(), loginForm.getPassword(), loginForm.getCookies());
    }

    private void getDocuments(String username, String password, Map<String, String> cookies) {
        getHtmlPages(username, password);
    }

    private void getHtmlPages(String username, String password) {
        username = username.trim();
        password = password.trim();

        Connection.Response response;
        String PHPSESSION;
        boolean authorized;

        // Get PHPSESSID cookie
        try {
            response = Jsoup.connect("https://sef.samos.aegean.gr")
                    .data("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .data("Accept-Encoding", "gzip, deflate, br")
                    .data("Accept-Language", "el-GR,el;q=0.8,en-US;q=0.5,en;q=0.3")
                    .data("Connection", "keep-alive")
                    .data("DNT", "1")
                    .data("Host", "sef.samos.aegean.gr")
                    .data("Upgrade-Insecure-Requests", "1")
                    .method(Connection.Method.GET)
                    .header("User-Agent", USER_AGENT)
                    .execute();

            PHPSESSION = response.cookie("PHPSESSID");
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException connException) {
            connected = false;
            logger.warn("Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("Error: {}", e.getMessage(), e);
            return;
        }

        // Attempt to connect
        try {
            response = Jsoup.connect("https://sef.samos.aegean.gr/authentication.php")
                    .data("username", username)
                    .data("password", password)
                    .data("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .data("Accept-Encoding", "gzip, deflate, br")
                    .data("Accept-Language", "el-GR,el;q=0.8,en-US;q=0.5,en;q=0.3")
                    .data("Connection", "keep-alive")
                    .data("Content-Length", "33")
                    .data("Content-Type", "application/x-www-form-urlencoded")
                    .data("PHPSESSID", PHPSESSION)
                    .data("DNT", "1")
                    .data("Host", "sef.samos.aegean.gr")
                    .data("Origin", "https://sef.samos.aegean.gr")
                    .data("Referer", "https://sef.samos.aegean.gr/")
                    .data("Upgrade-Insecure-Requests", "1")
                    .method(Connection.Method.GET)
                    .header("User-Agent", USER_AGENT)
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
    }

    private boolean authorizationCheck(Document document) {
        String html = document.toString();

        return !html.contains("Δεν έχετε δικαίωμα πρόσβασης στο Σ.Ε.Φ. Ελέγξτε τα στοιχεία σας και προσπαθήστε ξανά..");
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public boolean isConnected() {
        return connected;
    }
}
