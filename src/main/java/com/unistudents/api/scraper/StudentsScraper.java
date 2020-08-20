package com.unistudents.api.scraper;


import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StudentsScraper {

    private String username;
    private String password;
    private boolean authorized;
    private Document studentInfoPage;
    private Document gradesPage;

    public StudentsScraper(String username, String password) {
        this.username = username.trim().replace(" ", "");
        this.password = password.trim().replace(" ", "");
        this.authorized = false;
        this.getHtmlPages();
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public Document getStudentInfoPage() {
        return studentInfoPage;
    }

    public void setStudentInfoPage(Document studentInfoPage) {
        this.studentInfoPage = studentInfoPage;
    }

    public Document getGradesPage() {
        return gradesPage;
    }

    public void setGradesPage(Document gradesPage) {
        this.gradesPage = gradesPage;
    }

    public void getHtmlPages() {

        //
        // Request Login Html Page
        //

        Connection.Response response = null;
        String loginPage = "";
        String hashKey;
        String hashValue;
        String[] keyValue;

        do {
            try {
                response = getResponse();
                loginPage = String.valueOf(response.parse());
            } catch (IOException e) {
                e.printStackTrace();
            }

            // get hashed key, value
            keyValue = getKeyValue(loginPage);
        }
        while (keyValue == null);

        // get hashKey
        hashKey = keyValue[0];

        // get hashValue
        hashValue = keyValue[1];

        // store session cookies
        Map<String, String> sessionCookies = new HashMap<>();
        for (Map.Entry<String, String> entry : response.cookies().entrySet()) {
            if (entry.getKey().startsWith("ASPSESSIONID") || entry.getKey().startsWith("HASH_ASPSESSIONID")) {
                sessionCookies.put(entry.getKey(), entry.getValue());
            }
        }

        //
        // Try to Login
        //

        try {
            response = Jsoup.connect("https://students.unipi.gr/login.asp")
                    .data("userName", this.username)
                    .data("pwd", this.password)
                    .data("submit1", "%C5%DF%F3%EF%E4%EF%F2")
                    .data("loginTrue", "login")
                    .data(hashKey, hashValue)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9,el;q=0.8")
                    .header("Cache-Control", "max-age=0")
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Host", "students.unipi.gr")
                    .header("Origin", "https://students.unipi.gr")
                    .header("Referer", "https://students.unipi.gr/login.asp")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36")
                    .cookies(response.cookies())
                    .method(Connection.Method.POST)
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // returned document from login response
        Document returnedDoc = null;
        boolean authorized = false;
        try {
            returnedDoc = response.parse();
            authorized = authorizationCheck(returnedDoc);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //
        // if is not authorized return
        //
        if (!authorized) {
            this.authorized = authorized;
            return;
        }
        else {
            this.authorized = true;
        }

        // set student info page
        setStudentInfoPage(returnedDoc);

        // add cookies
        sessionCookies.put("rcva%5F", response.cookies().get("rcva%5F"));
        sessionCookies.put("HASH_rcva%5F", response.cookies().get("HASH_rcva%5F"));
        sessionCookies.put("login", "True");

        //
        // Request Grades Page
        //

        try {
            response = Jsoup.connect("https://students.unipi.gr/stud_CResults.asp")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9,el;q=0.8")
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Host", "students.unipi.gr")
                    .header("Referer", "https://students.unipi.gr/studentMain.asp")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36")
                    .method(Connection.Method.GET)
                    .cookies(sessionCookies)
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // set grades page
        try {
            setGradesPage(response.parse());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Connection.Response getResponse() {
        try {
            return Jsoup.connect("https://students.unipi.gr/login.asp")
                    .method(Connection.Method.GET)
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String decode(String hash) {
        hash = hash.replace("'", "").replace("+", "").replace("\\x", "").trim();
        byte[] decodedHash = new byte[0];
        try {
            decodedHash = Hex.decodeHex(hash.toCharArray());
        } catch (DecoderException e) {
            e.printStackTrace();
        }
        return new String(decodedHash);
    }

    private String[] getKeyValue(String loginPage) {
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
            } else {
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
}
