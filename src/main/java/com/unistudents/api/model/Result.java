package com.unistudents.api.model;

import java.util.Arrays;

public class Result {

    private int totalPassedCourses;
    private double totalAverageGrade;
    private int totalEcts;
    private Semester[] semesters;

    public Result() {
    }

    public Result(int totalPassedCourses, double totalAverageGrade, int totalEcts, Semester[] semesters) {
        this.totalPassedCourses = totalPassedCourses;
        this.totalAverageGrade = totalAverageGrade;
        this.totalEcts = totalEcts;
        this.semesters = semesters;
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

    public Semester[] getSemesters() {
        return semesters;
    }

    public void setSemesters(Semester[] semesters) {
        this.semesters = semesters;
    }

    @Override
    public String toString() {
        return "Result{" +
                "totalPassedCourses=" + totalPassedCourses +
                ", totalAverageGrade=" + totalAverageGrade +
                ", totalEcts=" + totalEcts +
                ", semesters=" + Arrays.toString(semesters) +
                '}';
    }
}
