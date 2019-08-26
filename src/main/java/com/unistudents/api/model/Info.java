package com.unistudents.api.model;

public class Info {

    private String aem;
    private String firstName;
    private String lastName;
    private String departure;
    private String semester;
    private String registrationYear;

    public Info() {
    }

    public Info(String aem, String firstName, String lastName, String departure, String semester, String registrationYear) {
        this.aem = aem;
        this.firstName = firstName;
        this.lastName = lastName;
        this.departure = departure;
        this.semester = semester;
        this.registrationYear = registrationYear;
    }

    public String getAem() {
        return aem;
    }

    public void setAem(String aem) {
        this.aem = aem;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDeparture() {
        return departure;
    }

    public void setDeparture(String departure) {
        this.departure = departure;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getRegistrationYear() {
        return registrationYear;
    }

    public void setRegistrationYear(String registrationYear) {
        this.registrationYear = registrationYear;
    }

    @Override
    public String toString() {
        return "Info{" +
                "aem='" + aem + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", departure='" + departure + '\'' +
                ", semester='" + semester + '\'' +
                ", registrationYear='" + registrationYear + '\'' +
                '}';
    }
}
