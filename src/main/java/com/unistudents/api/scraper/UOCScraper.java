package com.unistudents.api.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unistudents.api.common.UserAgentGenerator;
import com.unistudents.api.model.LoginForm;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class UOCScraper {
    private final String DOMAIN;
    private final String PRE_LOG;
    private final String USER_AGENT;
    private boolean connected;
    private boolean authorized;
    private String infoJSON;
    private String gradesJSON;
    private String totalAverageGrade;
    private Map<String, String> cookies;
    private final Logger logger = LoggerFactory.getLogger(UOCScraper.class);

    public UOCScraper(LoginForm loginForm, String university, String system, String domain) {
        this.DOMAIN = domain;
        this.PRE_LOG = university + (system == null ? "" : "." + system);
        this.USER_AGENT = UserAgentGenerator.generate();
        this.connected = true;
        this.authorized = true;
        this.getDocuments(loginForm.getUsername(), loginForm.getPassword(), loginForm.getCookies());
    }

    private void getDocuments(String username, String password, Map<String, String> cookies) {
        if (cookies == null) {
            getHtmlPages(username, password);
        ***REMOVED***
            getHtmlPages(cookies);
            if (infoJSON == null || gradesJSON == null || totalAverageGrade == null) {
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

        Connection.Response response;
        Document doc;
        String execution;
        Document pageIncludesToken;

        try {
            response = getResponse();
            if (response == null) return;
            doc = response.parse();
            Elements exec = doc.getElementsByAttributeValue("name", "execution");
            execution = exec.first().attributes().get("value");
        } catch (IOException e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
            return;
        }


        //
        // Try to Login
        //

        try {
            response = Jsoup.connect("https://sso.uoc.gr/login?service=https%3A%2F%2Feduportal.cict.uoc.gr%2Flogin%2Fcas")
                    .data("username", username)
                    .data("password", password)
                    .data("execution", execution)
                    .data("_eventId", "submit")
                    .data("geolocation", "")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Cache-Control", "max-age=0")
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Origin", "https://sso.uoc.gr")
                    .header("Referer", "https://sso.uoc.gr/login?service=https%3A%2F%2Feduportal.cict.uoc.gr%2Flogin%2Fcas")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "same-origin")
                    .header("Sec-Fetch-User", "?1")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("User-Agent", USER_AGENT)
                    .method(Connection.Method.POST)
                    .ignoreHttpErrors(true)
                    .execute();
            pageIncludesToken = response.parse();
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            connected = false;
            logger.warn("[" + PRE_LOG + "] Warning: {}", connException.getMessage(), connException);
            return;
        } catch (IOException e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
            return;
        }

        // authentication check
        if (response.statusCode() == 401) {
            this.authorized = false;
            return;
        }

        StringBuilder cookie = new StringBuilder();
        cookie.append("JSESSIONID");
        cookie.append("=");
        cookie.append(response.cookies().get("JSESSIONID"));

        Elements el;
        String _csrf;
        if (pageIncludesToken != null) {
            el = pageIncludesToken.getElementsByAttributeValue("name", "_csrf");
            _csrf = el.first().attributes().get("content");
        ***REMOVED***
            return;
        }

        //
        //  Http GET request to api endpoint
        //  using cookie form previous request
        //

        String profilesJSON = httpGET("https://" + DOMAIN + "/api/person/profiles", cookie.toString(), _csrf);
        if (profilesJSON == null) return;

        // get X-Profile variable from infoJSON
        String xProfile = getXProfile(profilesJSON);

        infoJSON = httpGET("https://" + DOMAIN + "/feign/student/student_data", cookie.toString(), _csrf, xProfile);
        if (infoJSON == null) return;

        gradesJSON = httpGET("https://" + DOMAIN + "/feign/student/grades/diploma", cookie.toString(), _csrf, xProfile);
        if (gradesJSON == null) return;

        totalAverageGrade = httpGET("https://" + DOMAIN + "/feign/student/grades/average_student_course_grades", cookie.toString(), _csrf, xProfile);
        setCookies(cookie.toString(), _csrf, xProfile);
    }

    private void getHtmlPages(Map<String, String> cookies) {
        String cookie = cookies.get("cookie");
        String _csrf = cookies.get("_csrf");
        String xProfile = cookies.get("xProfile");
        if (cookie == null ||
            _csrf == null ||
            xProfile == null) return;

        infoJSON = httpGET("https://" + DOMAIN + "/feign/student/student_data", cookie, _csrf, xProfile);
        if (infoJSON == null) return;

        gradesJSON = httpGET("https://" + DOMAIN + "/feign/student/grades/diploma", cookie, _csrf, xProfile);
        if (gradesJSON == null) return;

        totalAverageGrade = httpGET("https://" + DOMAIN + "/feign/student/grades/average_student_course_grades", cookie, _csrf, xProfile);
        setCookies(cookie, _csrf, xProfile);
    }

    private Connection.Response getResponse() {
        try {
            return Jsoup.connect("https://sso.uoc.gr/login?service=https%3A%2F%2Feduportal.cict.uoc.gr%2Flogin%2Fcas")
                    .method(Connection.Method.GET)
                    .header("User-Agent", USER_AGENT)
                    .execute();
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            connected = false;
            logger.warn("[" + PRE_LOG + "] Warning: {}", connException.getMessage(), connException);
        } catch (IOException e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
        }
        return null;
    }

    private String httpGET(String url, String cookie, String _csrf) {
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Cookie", cookie);
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("X-CSRF-TOKEN", _csrf);
            con.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // success
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        con.getInputStream()));
                String inputLine;
                StringBuffer responseString = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    responseString.append(inputLine);
                }
                in.close();
                con.disconnect();

                // print result
                return responseString.toString();
            ***REMOVED***
                return null;
            }
        }  catch (ConnectException | UnknownHostException | HttpStatusException connException) {
            connected = false;
            logger.warn("[" + PRE_LOG + "] Warning: {}", connException.getMessage(), connException);
        }  catch (Exception e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
        }
        return null;
    }

    private String httpGET(String url, String cookie, String _csrf, String xProfile) {
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");
            con.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            con.setRequestProperty("Cache-Control", "max-age=0");
            con.setRequestProperty("Connection", "keep-alive");
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            con.setRequestProperty("Host", DOMAIN);
            con.setRequestProperty("Referer", "https://" + DOMAIN  + "/student/grades/list_diploma?p=" + xProfile);
            con.setRequestProperty("Sec-Fetch-Dest", "empty");
            con.setRequestProperty("Sec-Fetch-Mode", "cors");
            con.setRequestProperty("Sec-Fetch-Site", "same-origin");
            con.setRequestProperty("Upgrade-Insecure-Requests", "1");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("X-CSRF-TOKEN", _csrf);
            con.setRequestProperty("X-Profile", xProfile);
            con.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            con.setRequestProperty("Cookie", cookie);
            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // success
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        con.getInputStream()));
                String inputLine;
                StringBuffer responseString = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    responseString.append(inputLine);
                }
                in.close();
                con.disconnect();

                // print result
                return responseString.toString();
            ***REMOVED***
                return null;
            }
        } catch (ConnectException | UnknownHostException | HttpStatusException connException) {
            connected = false;
            logger.warn("[" + PRE_LOG + "] Warning: {}", connException.getMessage(), connException);
        } catch (Exception e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
        }
        return null;
    }

    private String getXProfile(String profilesJSON) {
        try {
            JsonNode node = new ObjectMapper().readTree(profilesJSON);
            JsonNode studentProfiles = node.get("studentProfiles");
            for (JsonNode student: studentProfiles)  {
                return student.get("id").asText();
            }
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

    public String getInfoJSON() {
        return infoJSON;
    }

    public String getGradesJSON() {
        return gradesJSON;
    }

    public String getTotalAverageGrade() {
        return totalAverageGrade;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public void setCookies(String cookie, String _csrf, String xProfile) {
        this.cookies = new HashMap<>();
        cookies.put("cookie", cookie);
        cookies.put("_csrf", _csrf);
        cookies.put("xProfile", xProfile);
    }
}
