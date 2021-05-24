package com.unistudents.api.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unistudents.api.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class UNIVERSISParser {
    private Exception exception;
    private String document;
    private final String PRE_LOG;
    private final Logger logger = LoggerFactory.getLogger(UNIVERSISParser.class);

    public UNIVERSISParser(String university) {
        this.PRE_LOG = "[" + university + ".UNIVERSIS]";
    }

    private Info parseInfoJSON(String infoJSON) {
        Info info = new Info();

        try {
            JsonNode node = new ObjectMapper().readTree(infoJSON);

            JsonNode personInfo = node.get("person");
            String firstName = personInfo.get("familyName").asText();
            String lastName = personInfo.get("givenName").asText();
            String aem = node.get("studentIdentifier").asText();
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
        } catch (Exception e) {
            logger.error(this.PRE_LOG  + " Error: {}", e.getMessage(), e);
            setException(e);
            setDocument(infoJSON);
            return null;
        }
    }

    private Grades parseGradesJSON(String gradesJSON) {
        Grades grades = new Grades();
        ArrayList<Semester> semesters = initSemesters();
        DecimalFormat df2 = new DecimalFormat("#.##");

        try {
            JsonNode node = new ObjectMapper().readTree(gradesJSON);

            JsonNode courses = node.get("value");

            int totalPassedCourses = 0;
            float totalPassedCoursesSum = 0;
            int totalECTS = 0;
            for (int i = 0; i < semesters.size() - 1; i++) {
                Semester semester = semesters.get(i);
                int semesterPassedCourses = 0;
                float semesterPassedCoursesSum = 0;
                int semesterECTS = 0;
                for (JsonNode courseJSON : courses) {
                    Course course = new Course();
                    int courseSemester = courseJSON.get("semester").get("id").asInt();
                    if (i == courseSemester - 1) {
                        String id = courseJSON.get("course").get("displayCode").asText();
                        String name = courseJSON.get("course").get("name").asText();
                        String type = courseJSON.get("courseType").get("abbreviation").asText();
                        Float gradeToCompute = courseJSON.get("grade") != null ? courseJSON.get("grade").floatValue() * 10 : null;
                        String grade = gradeToCompute != null ? String.valueOf(gradeToCompute) : "-";

                        JsonNode examPeriodNode = courseJSON.get("examPeriod");
                        String examPeriod = "-";
                        if (examPeriodNode != null) {
                            examPeriod = courseJSON.get("examPeriod").get("alternateName").asText() + " " + courseJSON.get("lastRegistrationYear").get("alternateName").asText();
                        } else {
                            grade = "-";
                        }

                        int courseECTS = courseJSON.get("ects").asInt();
                        boolean isPassed = courseJSON.get("isPassed").asInt() == 1;
                        if (gradeToCompute != null && isPassed) {
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

                totalECTS += semesterECTS;
                totalPassedCoursesSum += semesterPassedCoursesSum;
                totalPassedCourses += semesterPassedCourses;

                semester.setEcts(String.valueOf(semesterECTS));
                semester.setPassedCourses(semesterPassedCourses);
                semester.setGradeAverage(semesterPassedCourses > 0 ? df2.format(semesterPassedCoursesSum / semesterPassedCourses) : "-");
            }

            grades.setSemesters(clearSemesters(semesters));

            grades.setTotalEcts(String.valueOf(totalECTS));
            grades.setTotalPassedCourses(String.valueOf(totalPassedCourses));
            grades.setTotalAverageGrade(totalPassedCourses > 0 ? df2.format(totalPassedCoursesSum / totalPassedCourses) : "-");
        } catch (Exception e) {
            logger.error(this.PRE_LOG  + " Error: {}", e.getMessage(), e);
            setException(e);
            setDocument(gradesJSON);
            return null;
        }

        return grades;
    }

    // Clear unwanted semesters from initialization.
    private ArrayList<Semester> clearSemesters(ArrayList<Semester> semesters) {
        Iterator<Semester> iterator = semesters.iterator();
        while (iterator.hasNext()) {
            Semester semester = (Semester) iterator.next();
            if (semester.getCourses().isEmpty()) {
                iterator.remove();
            }
        }

        return semesters;
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
            logger.error(this.PRE_LOG  + " Error: {}", e.getMessage(), e);
            setException(e);
            setDocument(infoJSON + "\n\n\n======\n\n\n" + gradesJSON);
            return null;
        }
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }
}
