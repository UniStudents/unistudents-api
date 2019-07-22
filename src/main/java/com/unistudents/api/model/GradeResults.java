package com.unistudents.api.model;

import java.util.ArrayList;

public class GradeResults {

    private int totalPassedCourses;
    private double totalAverageGrade;
    private int totalEcts;
    private ArrayList<Semester> semesters;

    public GradeResults() {
        this.semesters = new ArrayList<>();
    }

    public GradeResults(int totalPassedCourses, double totalAverageGrade, int totalEcts) {
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

    public double getTotalAverageGrade() {
        return totalAverageGrade;
    }

    public void setTotalAverageGrade(double totalAverageGrade) {
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
        return "GradeResults{" +
                "totalPassedCourses=" + totalPassedCourses +
                ", totalAverageGrade=" + totalAverageGrade +
                ", totalEcts=" + totalEcts +
                ", semesters=" + semesters +
                '}';
    }
}
