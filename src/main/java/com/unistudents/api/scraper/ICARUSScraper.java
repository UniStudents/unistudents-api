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

public class ICARUSScraper {
    private final String USER_AGENT;
    private boolean connected;
    private boolean authorized;
    private Document infoAndGradePage;
    private Map<String, String> cookies;
    private final Logger logger = LoggerFactory.getLogger(ICARUSScraper.class);

    public ICARUSScraper(LoginForm loginForm) {
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
            if (infoAndGradePage == null) {
                getHtmlPages(username, password);
            }
        }
    }

    private void getHtmlPages(String username, String password) {
        username = username.trim().replace(" ", "");
        password = password.trim().replace(" ", "");

        //
        // Request Login Html Page
        //

        Connection.Response response = null;

        try {
            response = Jsoup.connect("https://icarus-icsd.aegean.gr/")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9,el;q=0.8")
                    .header("Connection", "keep-alive")
                    .header("Host", "icarus-icsd.aegean.gr")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Sec-Fetch-User", "?1")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("User-Agent", USER_AGENT)
                    .method(Connection.Method.GET)
                    .execute();
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException connException) {
            connected = false;
            logger.warn("[AEGEAN.ICARUS] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[AEGEAN.ICARUS] Error: {}", e.getMessage(), e);
            return;
        }

        Map<String, String> cookies = response.cookies();

        //
        // Try to login
        //

        try {
            response = Jsoup.connect("https://icarus-icsd.aegean.gr/authentication.php")
                    .data("username", username)
                    .data("pwd", password)
                    .data("edit.x", "14")
                    .data("edit.y", "10")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9,el;q=0.8")
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Host", "icarus-icsd.aegean.gr")
                    .header("Origin", "https://icarus-icsd.aegean.gr")
                    .header("Referer", "https://icarus-icsd.aegean.gr/")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Sec-Fetch-User", "?1")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("User-Agent", USER_AGENT)
                    .cookies(cookies)
                    .method(Connection.Method.POST)
                    .execute();
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException connException) {
            connected = false;
            logger.warn("[AEGEAN.ICARUS] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[AEGEAN.ICARUS] Error: {}", e.getMessage(), e);
            return;
        }

        try {
            Document document = response.parse();
            if (document.select("#header_login u").text().trim().isEmpty()) {
                authorized = false;
                return;
            }
            setInfoAndGradePage(document);
            setCookies(cookies);
        } catch (IOException e) {
            logger.error("[AEGEAN.ICARUS] Error: {}", e.getMessage(), e);
        }
    }

    private void getHtmlPages(Map<String, String> cookies) {
        Connection.Response response;

        //
        // Request html document
        //

        try {
            response = Jsoup.connect("https://icarus-icsd.aegean.gr")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9,el;q=0.8")
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Host", "icarus-icsd.aegean.gr")
                    .header("Origin", "https://icarus-icsd.aegean.gr")
                    .header("Referer", "https://icarus-icsd.aegean.gr/")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Sec-Fetch-User", "?1")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("User-Agent", USER_AGENT)
                    .cookies(cookies)
                    .method(Connection.Method.GET)
                    .execute();
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException connException) {
            connected = false;
            logger.warn("[AEGEAN.ICARUS] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[AEGEAN.ICARUS] Error: {}", e.getMessage(), e);
            return;
        }

        try {
            Document document = response.parse();
            if (document.select("#header_login u").text().trim().isEmpty()) return;
            setInfoAndGradePage(document);
            setCookies(cookies);
        } catch (IOException e) {
            logger.error("[AEGEAN.ICARUS] Error: {}", e.getMessage(), e);
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

    public Document getInfoAndGradePage() {
        return infoAndGradePage;
    }

    public void setInfoAndGradePage(Document infoAndGradePage) {
        this.infoAndGradePage = infoAndGradePage;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }
}
