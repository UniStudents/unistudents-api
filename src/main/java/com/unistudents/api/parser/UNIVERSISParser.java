package com.unistudents.api.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unistudents.api.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.*;

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
            int totalPassedCoursesWithoutGrades = 0;
            float totalPassedCoursesSum = 0;
            double totalECTS = 0;
            for (Semester semester: semesters) {
                int semesterPassedCourses = 0;
                float semesterPassedCoursesSum = 0;
                double semesterECTS = 0;
                for (JsonNode courseJSON : courses) {
                    Course course = new Course();
                    int courseSemester = courseJSON.get("semester").get("id").asInt();
                    if (semester.getId() == courseSemester) {
                        String id = courseJSON.get("course").get("displayCode").asText();
                        String name = courseJSON.get("courseTitle").asText();
                        String type = courseJSON.get("courseType").get("abbreviation").asText();

                        String grade;
                        double gradeToCompute = -1;
                        String formattedGradeString = courseJSON.get("formattedGrade").asText().replace(",", ".");
                        switch (formattedGradeString) {
                            case "null":
                                grade = "-";
                                break;
                            case "ΕΠΙΤ":
                                grade = "P";
                                break;
                            case "ΑΠΟΤ":
                                grade = "F";
                                break;
                            default:
                                grade = formattedGradeString;
                                gradeToCompute = Double.parseDouble(grade);
                                break;
                        }

                        JsonNode examPeriodNode = courseJSON.get("examPeriod");
                        JsonNode lastRegistrationYear = courseJSON.get("lastRegistrationYear");
                        JsonNode gradeYear = courseJSON.get("gradeYear");
                        JsonNode registrationType = courseJSON.get("registrationType");
                        String examPeriod = "-";
                        if (examPeriodNode != null && lastRegistrationYear != null) {
                            examPeriod = examPeriodNode.get("alternateName").asText() + " " + lastRegistrationYear.get("alternateName").asText();
                        } else if (examPeriodNode != null && gradeYear != null) {
                            examPeriod = examPeriodNode.get("alternateName").asText() + " " + gradeYear.get("alternateName").asText();
                        } else if( registrationType.asInt() == 1 ) {
                            // this is when a student has an exemption on a course e.g. due to a transfer.
                            // an exemption can either include a grade or not, but it will always include
                            // the academic period in which the student passed/got the exemption. in
                            // any case, it should be counted as passed.
                            examPeriod = examPeriodNode != null ? examPeriodNode.get("alternateName").asText() + " " + gradeYear.get("alternateName").asText() : "-";
                        } else {
                            grade = "-";
                        }

                        double courseECTS = courseJSON.get("ects").asDouble();
                        boolean isPassed = courseJSON.get("isPassed").asInt() == 1;
                        if (gradeToCompute != -1 && isPassed && courseJSON.get("course").get("parentCourse").asText().equals("null")) {
                            semesterPassedCourses++;
                            semesterPassedCoursesSum += gradeToCompute;
                            semesterECTS += courseECTS;
                        } else if (gradeToCompute == -1 && isPassed) {
                            totalPassedCoursesWithoutGrades++;
                            semesterECTS += courseECTS;
                        }

                        course.setExamPeriod(examPeriod);
                        course.setType(type);
                        course.setId(id);
                        course.setName(name);
                        course.setGrade(grade.replace(".0", ""));

                        semester.getCourses().add(course);
                    }
                }

                totalECTS += semesterECTS;
                totalPassedCoursesSum += semesterPassedCoursesSum;
                totalPassedCourses += semesterPassedCourses;
                double semesterAverageGrade = -1;
                if (semesterPassedCourses > 0) {
                    semesterAverageGrade = (double) Math.round((semesterPassedCoursesSum / semesterPassedCourses) * 100.0) / 100.0;
                }

                Collections.sort(semester.getCourses(), new Comparator<Course>() {
                    @Override
                    public int compare(Course c1, Course c2) {
                        if (c1.getId().compareTo(c2.getId()) == 0) {
                            return c1.getName().compareTo(c2.getName());
                        } else{
                            return c1.getId().compareTo(c2.getId());
                        }
                    }
                });
                semester.setEcts(String.valueOf(semesterECTS));
                semester.setPassedCourses(semesterPassedCourses);
                semester.setGradeAverage(semesterAverageGrade != -1 ? df2.format(semesterAverageGrade) : "-");
            }

            grades.setSemesters(clearSemesters(semesters));
            grades.setTotalEcts(String.valueOf(df2.format(totalECTS)).replace(",", ".").replace(".0", ""));
            grades.setTotalPassedCourses(String.valueOf(totalPassedCourses + totalPassedCoursesWithoutGrades));
            double totalAverageGrade = -1;
            if (totalPassedCourses > 0) {
                totalAverageGrade = (double) Math.round((totalPassedCoursesSum / totalPassedCourses) * 100) / 100;
            }
            grades.setTotalAverageGrade(totalAverageGrade != -1 ? df2.format(totalAverageGrade) : "-");
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
        Semester[] semesters = new Semester[15];
        for (int i = 1; i <= 12; i++) {
            semesters[i - 1] = new Semester();
            semesters[i - 1].setId(i);
            semesters[i - 1].setPassedCourses(0);
            semesters[i - 1].setGradeAverage("-");
            semesters[i - 1].setCourses(new ArrayList<>());
        }
        semesters[12] = new Semester(251, 0, "", ""); // general winter semester. this is mostly used in departments like phed
        semesters[13] = new Semester(252, 0, "", ""); // general spring semester.
        semesters[14] = new Semester(255, 0, "", ""); // this is used for courses that can be taken either in spring or fall semester
        return new ArrayList<>(Arrays.asList(semesters));
    }

    public Student parseInfoAndGradesJSON(String infoJSON, String gradesJSON) {
        Student student = new Student();

        try {
            Info info = parseInfoJSON(infoJSON);
            Grades grades = parseGradesJSON(gradesJSON);

            if (info == null || grades == null) {
                return null;
            }

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
