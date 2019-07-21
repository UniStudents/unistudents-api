package com.unistudents.api.scraper;

public class StudentsScraper {

    private String username;
    private String password;
    private String html;

    public StudentsScraper() {
    }

    public StudentsScraper(String username, String password, String html) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHtml() {
        return html;
    }
}
