package com.unistudents.api.scraper;


import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StudentsScraper {

    private String username;
    private String password;

    public StudentsScraper() {
    }

    public StudentsScraper(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getHtml() {

        //
        // Request Login Html Page
        //

        Connection.Response response = null;
        String loginPage = "";
        try {
            response = Jsoup.connect("https://students.unipi.gr/login.asp")
                    .method(Connection.Method.GET)
                    .execute();
            loginPage = String.valueOf(response.parse());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // get hashKey
        String hashKey = getHashKey(loginPage);
        // get hashValue
        String hashValue = getHashValue(loginPage);

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

            System.out.println("Username: " + this.username);
            System.out.println("Password: " + this.password);

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

        try {
            return String.valueOf(response.parse());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getHashKey(String loginPage) {
        String hashKey = loginPage.substring(loginPage.indexOf("[2], '") + 6, loginPage.indexOf("')["));
        return decode(hashKey);
    }

    private String getHashValue(String loginPage) {
        String hashValue = loginPage.substring(loginPage.indexOf("[0], '") + 6, loginPage.indexOf("'); $"));
        return decode(hashValue);
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
}
