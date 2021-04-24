package com.unistudents.api.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unistudents.api.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class UNIVERSISParser {
    private final Logger logger = LoggerFactory.getLogger(UNIVERSISParser.class);

    private Info parseInfoJSON(String infoJSON) {
        Info info = new Info();

        try {
            JsonNode node = new ObjectMapper().readTree(infoJSON);

            JsonNode personInfo = node.get("person");
            String firstName = personInfo.get("familyName").asText();
            String lastName = personInfo.get("givenName").asText();
            String aem = "?";
            String registrationYear = node.get("inscriptionYear").get("alternateName").asText();
            String semester = node.get("semester").asText();
            String department = node.get("department").get("name").asText();

            info.setSemester(semester);
            info.setAem(aem);
            info.setDepartment(department);
            info.setFirstName(firstName);
            info.setLastName(lastName);
            info.setRegistrationYear(registrationYear);

            return info;
        } catch (IOException e) {
            logger.error("[UNIVERSIS] Error: {}", e.getMessage(), e);
            return null;
        }
    }

    private Grades parseGradesJSON(String gradesJSON) {
        Grades grades = new Grades();
        ArrayList<Semester> semesters = initSemesters();

        try {
            JsonNode node = new ObjectMapper().readTree(gradesJSON);

            JsonNode courses = node.get("value");

            for (int i = 0; i <semesters.size() - 1 ; i++) {
                Semester semester = semesters.get(i);
                int semesterPassedCourses = 0;
                float semesterPassedCoursesSum = 0;
                int semesterECTS = 0;
                for (JsonNode courseJSON: courses)  {
                    Course course = new Course();
                    int courseSemester = courseJSON.get("semester").get("id").asInt();
                    if (i == courseSemester - 1) {
                        String id = courseJSON.get("course").get("displayCode").asText();
                        String name = courseJSON.get("course").get("name").asText();
                        String type = courseJSON.get("courseType").get("abbreviation").asText();
                        Float gradeToCompute = courseJSON.get("grade") != null ? courseJSON.get("grade").floatValue() * 10 : null;
                        String grade = gradeToCompute != null ? String.valueOf(gradeToCompute) : "-";

                        JsonNode examPeriodNode = courseJSON.get("examPeriod");
                        String examPeriod = null;
                        if (examPeriodNode != null) {
                            examPeriod = courseJSON.get("examPeriod").get("alternateName").asText() + " " + courseJSON.get("lastRegistrationYear").get("alternateName").asText();
                        }

                        int courseECTS = courseJSON.get("ects").asInt();
                        if (gradeToCompute != null && gradeToCompute >= 5 && gradeToCompute <= 10) {
                            semesterPassedCourses++;
                            semesterPassedCoursesSum += gradeToCompute;
                            semesterECTS += courseECTS;
                        }

                        course.setExamPeriod(examPeriod);
                        course.setType(type);
                        course.setId(id);
                        course.setName(name);
                        course.setGrade(grade);

                        semester.getCourses().add(course);
                    }
                }
                semester.setEcts(String.valueOf(semesterECTS));
                semester.setPassedCourses(semesterPassedCourses);

                if (semesterPassedCourses > 0) {
                    semester.setGradeAverage(String.valueOf(semesterPassedCoursesSum/semesterPassedCourses));
                }
            }
        } catch (IOException e) {
            logger.error("[UNIVERSIS] Error: {}", e.getMessage(), e);
            return null;
        }

        return grades;
    }

    // Initialize semesters.
    private ArrayList<Semester> initSemesters() {
        Semester[] semesters = new Semester[12];
        for (int i = 1; i <= 12; i++) {
            semesters[i - 1] = new Semester();
            semesters[i - 1].setId(i);
            semesters[i - 1].setPassedCourses(0);
            semesters[i - 1].setGradeAverage("-");
            semesters[i - 1].setCourses(new ArrayList<>());
        }
        return new ArrayList<>(Arrays.asList(semesters));
    }

    public Student parseInfoAndGradesJSON(String infoJSON, String gradesJSON) {
        Student student = new Student();

        try {
            Info info = parseInfoJSON(infoJSON);

            if (info == null) return null;

            Grades grades = parseGradesJSON(gradesJSON);

            student.setInfo(info);
            student.setGrades(grades);
            return student;
        } catch (Exception e) {
            logger.error("[UNIVERSIS] Error: {}", e.getMessage(), e);
            return null;
        }
    }
}
