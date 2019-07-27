package com.unistudents.api.model;

import java.util.ArrayList;

public class GradeResults {

    private Student student;
    private int totalPassedCourses;
    private String totalAverageGrade;
    private int totalEcts;
    private ArrayList<Semester> semesters;

    public GradeResults() {
        this.semesters = new ArrayList<>();
    }

    public GradeResults(Student student, int totalPassedCourses, String totalAverageGrade, int totalEcts) {
        this.student = student;
        this.totalPassedCourses = totalPassedCourses;
        this.totalAverageGrade = totalAverageGrade;
        this.totalEcts = totalEcts;
        this.semesters = new ArrayList<>();
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
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
        return "GradeResults{" +
                "student=" + student +
                ", totalPassedCourses=" + totalPassedCourses +
                ", totalAverageGrade='" + totalAverageGrade + '\'' +
                ", totalEcts=" + totalEcts +
                ", semesters=" + semesters +
                '}';
    }
}
