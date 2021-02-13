package com.unistudents.api.scraper;


import com.unistudents.api.common.Services;
import com.unistudents.api.common.UserAgentGenerator;
import com.unistudents.api.model.LoginForm;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
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

public class CardisoftScraper {
    private final String SYSTEM;
    private final String DOMAIN;
    private final String URL;
    private final String PRE_LOG;
    private final String USER_AGENT;
    private boolean connected;
    private boolean authorized;
    private Document studentInfoPage;
    private Document gradesPage;
    private Map<String, String> cookies;
    private final Logger logger = LoggerFactory.getLogger(CardisoftScraper.class);

    public CardisoftScraper(LoginForm loginForm, String university, String system, String domain, String pathURL, boolean SSL) {
        this.SYSTEM = system;
        this.DOMAIN = domain;
        this.URL = ((SSL) ? "https://" : "http://") + domain + pathURL;
        this.PRE_LOG = university + (system == null ? "" : "." + system);
        this.connected = true;
        this.authorized = true;
        USER_AGENT = UserAgentGenerator.generate();
        getDocuments(loginForm.getUsername(), loginForm.getPassword(), loginForm.getCookies());
    }

    private void getDocuments(String username, String password, Map<String, String> cookies) {
        if (cookies == null) {
            getHtmlPages(username, password);
        ***REMOVED***
            getHtmlPages(cookies);
            if (studentInfoPage == null || gradesPage == null) {
                getHtmlPages(username, password);
            }
        }
    }

    private void getHtmlPages(String username, String password) {
        username = username.trim();
        password = password.trim();

        //
        // Request Login Html Page
        //

        Connection.Response response = null;
        String loginPage = "";
        String[] keyValue;

        try {
            response = getResponse(USER_AGENT);

            // check for connection errors
            if (response == null) return;

            loginPage = String.valueOf(response.parse());
        } catch (IOException e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
        }

        // get hashed key, value. if exists
        keyValue = getKeyValue(loginPage);

        // store data to pass
        HashMap<String, String> data = new HashMap<>();
        data.put((SYSTEM != null) ? (SYSTEM.equals("CM") ? "userName1" : "userName") : "userName", username);
        data.put("pwd", password);
        data.put("submit1", "%C5%DF%F3%EF%E4%EF%F2");
        data.put("loginTrue", "login");
        if (keyValue != null)
            if (keyValue.length == 2)
                if (!keyValue[0].isEmpty() && !keyValue[1].isEmpty())
                    data.put(keyValue[0], keyValue[1]);

        // store session cookies
        Map<String, String> cookies = new HashMap<>();
        for (Map.Entry<String, String> entry : response.cookies().entrySet()) {
            if (entry.getKey().startsWith("ASPSESSIONID") || entry.getKey().startsWith("HASH_ASPSESSIONID")) {
                cookies.put(entry.getKey(), entry.getValue());
            }
        }

        //
        // Try to Login
        //

        try {
            response = Jsoup.connect(URL + "/login.asp")
                    .data(data)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9,el;q=0.8")
                    .header("Cache-Control", "max-age=0")
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Host", DOMAIN)
                    .header("Origin", URL)
                    .header("Referer", URL + "/login.asp")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("User-Agent", USER_AGENT)
                    .cookies(response.cookies())
                    .method(Connection.Method.POST)
                    .execute();
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            connected = false;
            logger.warn("[" + PRE_LOG + "] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
            return;
        }

        // returned document from login response
        Document returnedDoc = null;
        boolean authorized = false;
        try {
            returnedDoc = response.parse();
            authorized = authorizationCheck(returnedDoc);
        } catch (IOException e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
        }

        //
        // if is not authorized return
        //
        if (!authorized) {
            this.authorized = false;
            return;
        }
        else {
            this.authorized = true;
        }

        // if server is not responding
        if (returnedDoc.toString().contains("The LDAP server is unavailable. (AuthPublisherObject)")) {
            connected = false;
            return;
        }

        // set student info page
        setStudentInfoPage(returnedDoc);

        // add cookies
        for (Map.Entry<String, String> entry : response.cookies().entrySet()) {
            cookies.put(entry.getKey(), entry.getValue());
        }

        //
        // Request Grades Page
        //

        try {
            response = Jsoup.connect(URL + "/stud_CResults.asp")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9,el;q=0.8")
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Host", DOMAIN)
                    .header("Referer", URL + "/studentMain.asp")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("User-Agent", USER_AGENT)
                    .cookies(cookies)
                    .method(Connection.Method.GET)
                    .execute();
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            connected = false;
            logger.warn("[" + PRE_LOG + "] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
            return;
        }

        // set grades page
        try {
            setGradesPage(response.parse());
            setCookies(cookies);
        } catch (IOException e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
        }
    }

    private void getHtmlPages(Map<String, String> cookies) {
        Connection.Response response;

        //
        // Request Info Page
        //

        try {
            response = Jsoup.connect(URL + "/studentMain.asp")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9,el;q=0.8")
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Host", DOMAIN)
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("User-Agent", USER_AGENT)
                    .cookies(cookies)
                    .followRedirects(false)
                    .method(Connection.Method.GET)
                    .execute();
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            connected = false;
            logger.warn("[" + PRE_LOG + "] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
            return;
        }

        // set info page
        try {
            if (response.statusCode() != 200) return;
            Document infoPage = response.parse();
            setStudentInfoPage(infoPage);
        } catch (IOException e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
        }

        //
        // Request Grades Page
        //

        try {
            response = Jsoup.connect(URL + "/stud_CResults.asp")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9,el;q=0.8")
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Host", DOMAIN)
                    .header("Referer", URL + "/studentMain.asp")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("User-Agent", USER_AGENT)
                    .cookies(cookies)
                    .method(Connection.Method.GET)
                    .execute();
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            connected = false;
            logger.warn("[" + PRE_LOG + "] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
            return;
        }

        // set grades page
        try {
            setGradesPage(response.parse());
            setCookies(cookies);
        } catch (IOException e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
        }
    }

    private Connection.Response getResponse(String userAgent) {
        try {
            return Jsoup.connect(URL + "/login.asp")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9,el;q=0.8")
                    .header("Connection", "keep-alive")
                    .header("Host", DOMAIN)
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Sec-Fetch-User", "?1")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("User-Agent", userAgent)
                    .method(Connection.Method.GET)
                    .execute();
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            connected = false;
            logger.warn("[" + PRE_LOG + "] Warning: {}", connException.getMessage(), connException);
        } catch (IOException e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
        }
        return null;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isAuthorized() {
        return authorized;
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

    private String[] getKeyValue(String loginPage) {
        if (SYSTEM != null) {
            if (SYSTEM.equals("TEITHE") || SYSTEM.equals("CM")) {
                return new String[0];
            }
        }

        if (loginPage.contains("eval([]")) {
            return getObfuscatedTypeTwo(loginPage);
        ***REMOVED***
            return getObfuscatedTypeOne(loginPage);
        }
    }

    private String[] getObfuscatedTypeOne(String loginPage) {
        String[] keyValue = new String[2];

        try {
            int keyIndex = loginPage.indexOf("], '");
            int valueIndex = loginPage.lastIndexOf("], '");

            if (keyIndex != -1 && keyIndex != valueIndex) {
                int lastKeyIndex = getLastCharIndex(loginPage, keyIndex, ')');
                int lastValueIndex = getLastCharIndex(loginPage, valueIndex, ')');

                keyValue[0] = decode(loginPage.substring(keyIndex + 4, lastKeyIndex - 1) );
                keyValue[1] = decode(loginPage.substring(valueIndex + 4, lastValueIndex - 1) );
            } else if (keyIndex != -1) {
                keyIndex = loginPage.indexOf("'input[name=");

                int lastKeyIndex = getLastCharIndex(loginPage, keyIndex, ')');
                int lastValueIndex = getLastCharIndex(loginPage, valueIndex, ')');

                keyValue[0] = decode(loginPage.substring(keyIndex + 12, lastKeyIndex - 2) );
                keyValue[1] = decode(loginPage.substring(valueIndex + 4, lastValueIndex - 1) );
            ***REMOVED***
                keyIndex = loginPage.indexOf("name=\"\\");
                valueIndex = loginPage.indexOf("value=\"\\");

                int lastKeyIndex = getLastCharIndex(loginPage, keyIndex, '"');
                int lastValueIndex = getLastCharIndex(loginPage, valueIndex, '"');

                keyValue[0] = decode(loginPage.substring(keyIndex + 6, lastKeyIndex) );
                keyValue[1] = decode(loginPage.substring(valueIndex + 7, lastValueIndex) );
            }
            return keyValue;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String[] getObfuscatedTypeTwo(String loginPage) {
        String obfuscatedString = loginPage.substring(loginPage.indexOf("eval(") + 5, loginPage.indexOf("());</script>") + 2);
        return Services.jsUnFuck(obfuscatedString);
    }

    private String decode(String hash) {
        hash = hash.replace("'", "").replace("+", "").replace("\\x", "").trim();
        byte[] decodedHash = new byte[0];
        try {
            decodedHash = Hex.decodeHex(hash.toCharArray());
        } catch (DecoderException e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
        }
        return new String(decodedHash);
    }

    private int getLastCharIndex(String content, int index, char c) {
        int i = index + 40;
        while (true) {
            Character character = content.charAt(i);
            if (character.equals(c)) {
                return i;
            }
            i++;
        }
    }

    private boolean authorizationCheck(Document document) {

        String html = document.toString();

        return !(html.contains("Λάθος όνομα χρήστη ή κωδικού πρόσβασης") ||
                html.contains("Λάθος όνομα χρήστη") ||
                html.contains("Ο χρήστης δεν έχει πρόσβαση στην εφαρμογή"));
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }
}
