package com.unistudents.api.model;

import java.util.ArrayList;

public class Grades {

    private int totalPassedCourses;
    private String totalAverageGrade;
    private int totalEcts;
    private ArrayList<Semester> semesters;

    public Grades() {
        this.semesters = new ArrayList<>();
    }

    public Grades(int totalPassedCourses, String totalAverageGrade, int totalEcts) {
        this.totalPassedCourses = totalPassedCourses;
        this.totalAverageGrade = totalAverageGrade;
        this.totalEcts = totalEcts;
        this.semesters = new ArrayList<>();
    }

    public int getTotalPassedCourses() {
        return totalPassedCourses;
    }

    public void setTotalPassedCourses(int totalPassedCourses) {
        this.totalPassedCourses = totalPassedCourses;
    }

    public String getTotalAverageGrade() {
        return totalAverageGrade;
    }

    public void setTotalAverageGrade(String totalAverageGrade) {
        this.totalAverageGrade = totalAverageGrade;
    }

    public int getTotalEcts() {
        return totalEcts;
    }

    public void setTotalEcts(int totalEcts) {
        this.totalEcts = totalEcts;
    }

    public ArrayList<Semester> getSemesters() {
        return semesters;
    }

    public void setSemesters(ArrayList<Semester> semesters) {
        this.semesters = semesters;
    }

    @Override
    public String toString() {
        return "Grades{" +
                "totalPassedCourses=" + totalPassedCourses +
                ", totalAverageGrade='" + totalAverageGrade + '\'' +
                ", totalEcts=" + totalEcts +
                ", semesters=" + semesters +
                '}';
    }
}
