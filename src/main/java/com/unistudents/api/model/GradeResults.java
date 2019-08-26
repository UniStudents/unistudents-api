package com.unistudents.api.model;

import java.util.ArrayList;

public class GradeResults {

    private StudentObj studentObj;
    private int totalPassedCourses;
    private String totalAverageGrade;
    private int totalEcts;
    private ArrayList<Semester> semesters;

    public GradeResults() {
        this.semesters = new ArrayList<>();
    }

    public GradeResults(StudentObj studentObj, int totalPassedCourses, String totalAverageGrade, int totalEcts) {
        this.studentObj = studentObj;
        this.totalPassedCourses = totalPassedCourses;
        this.totalAverageGrade = totalAverageGrade;
        this.totalEcts = totalEcts;
        this.semesters = new ArrayList<>();
    }

    public StudentObj getStudentObj() {
        return studentObj;
    }

    public void setStudentObj(StudentObj studentObj) {
        this.studentObj = studentObj;
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
                "studentObj=" + studentObj +
                ", totalPassedCourses=" + totalPassedCourses +
                ", totalAverageGrade='" + totalAverageGrade + '\'' +
                ", totalEcts=" + totalEcts +
                ", semesters=" + semesters +
                '}';
    }
}
