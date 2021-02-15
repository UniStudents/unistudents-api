package com.unistudents.api.parser;

import com.unistudents.api.common.StringHelper;
import com.unistudents.api.model.*;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SEFParser {

    private final Logger logger = LoggerFactory.getLogger(SEFParser.class);

    private Info parseInfoPage(Document infoPage, Document gradesPage) {
        Info info = new Info();

        try {
            String aem = infoPage.select("input[name=\"am\"]").text();
            String firstName = infoPage.select("input[name=\"fname\"]").text();
            String lastName = infoPage.select("input[name=\"sname\"]").text();

            String universityName = infoPage.select(".navbar-brand > b").text();
            String fullName = infoPage.select(".navbar-brand").text();
            String department = StringHelper.removeTones(fullName.replace(universityName, "").trim().toUpperCase());

            int registrationYear = getRegistrationYear(gradesPage);
            int semester = getCurrentSemester(registrationYear);

            info.setAem(aem);
            info.setFirstName(firstName);
            info.setLastName(lastName);
            info.setDepartment(department);
            info.setSemester(String.valueOf(semester));
            info.setRegistrationYear(String.valueOf(registrationYear));

            return info;
        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage(), e);
            return null;
        }
    }

    private Grades parseGradesPage(Document gradesPage, int registrationYear) {
        Grades grades = new Grades();
        Elements declaredSubjectsDOM = gradesPage.select("#tab_2 > table > tbody > tr");
        ArrayList<Semester> declaredCourses = getDeclaredCourses(declaredSubjectsDOM, registrationYear);

        Elements passedSubjects = gradesPage.select("#tab_3 table tr");

        return grades;
    }

    public Student parseInfoAndGradesPages(Document infoPage, Document gradesPage) {
        Student student = new Student();

        try {
            Info info = parseInfoPage(infoPage, gradesPage);

            // We have to pass registration year in order to calculate invalid semester value.
            Grades grades = parseGradesPage(gradesPage, Integer.parseInt(info.getRegistrationYear()));

            if (info == null || grades == null) {
                return null;
            }

            student.setInfo(info);
            student.setGrades(grades);

            return student;
        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage(), e);
            return null;
        }
    }

    // Get all declared courses.
    private ArrayList<Semester> getDeclaredCourses(Elements declaredSubjectsDOM, int registrationYear) {
        ArrayList<Semester> declaredSemesters = initSemesters();
        ArrayList<String> insertedCourses = new ArrayList<>();

        for (int i = declaredSubjectsDOM.size() - 1; i >= 0; i--) {
            Elements course = declaredSubjectsDOM.get(i).select("td");
            String courseId = course.get(0).text();
            String courseExamPeriod = course.get(8).text();

            String semesterId;
            if ((isSemesterValid(course.get(2).text()))) {
                semesterId = String.valueOf(Integer.parseInt(course.get(2).text()));
            } else {
                semesterId = String.valueOf(getSemesterFromExamPeriod(courseExamPeriod, registrationYear));
            }

            if (!insertedCourses.contains(courseId)) {
                insertedCourses.add(courseId);

                String courseName = course.get(1).text();
                String courseType = course.get(5).text();

                Course courseObj = new Course();
                courseObj.setId(courseId);
                courseObj.setName(courseName);
                courseObj.setGrade("-");
                courseObj.setExamPeriod(courseExamPeriod);
                courseObj.setType(courseType);

                Semester semester = declaredSemesters.get(Integer.parseInt(semesterId) - 1);
                semester.getCourses().add(courseObj);
            }
        }

        return clearSemesters(declaredSemesters);
    }

    // Clear unwanted semesters from initialization.
    private ArrayList<Semester> clearSemesters(ArrayList<Semester> semesters) {
        Iterator iterator = semesters.iterator();
        while (iterator.hasNext()) {
            Semester semester = (Semester) iterator.next();
            if (semester.getCourses().isEmpty()) {
                iterator.remove();
            }
        }

        return semesters;
    }

    // Check if semester is valid.
    private boolean isSemesterValid(String semester) {
        try {
            int _semester = Integer.parseInt(semester);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
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

    // Get registration year from the first course declaration.
    private int getRegistrationYear(Document gradesPage) {
        Elements firstCourse = gradesPage.select("#tab_2 > table > tbody > tr").get(0).select("td");
        String firstCourseRegistration = firstCourse.last().text();
        return Integer.parseInt(firstCourseRegistration.replaceAll("\\D+", "").substring(0, 4));
    }

    // Get current student's semester, based on current month and year.
    private int getCurrentSemester(int registrationYear) {
        LocalDate currentDate = LocalDate.now();
        int currentMonth = currentDate.getMonth().getValue();

        int currentYear = 0;
        if (currentMonth >= 1 && currentMonth <= 9) {
            currentYear = currentDate.getYear() - 1;
        } else {
            currentYear = currentDate.getYear();
        }

        String currentPeriod = "";
        if (currentMonth >= 2 && currentMonth <= 8) {
            currentPeriod = "Εαρινό " + currentYear;
        } else if (currentMonth >= 9 && currentMonth <= 12 || currentMonth == 1) {
            currentPeriod = "Χειμερινό " + currentYear;
        }

        return getSemesterFromExamPeriod(currentPeriod, registrationYear);
    }

    // Get semester from exam period. Necessary for some subjects with semester value: από μαθηματικό.
    private int getSemesterFromExamPeriod(String examPeriod, int registrationYear) {
        String coursePeriod = examPeriod.split(" ")[0];
        int coursePeriodNumber = ((coursePeriod.equals("Χειμερινό")) ? 1 : 0);
        int courseYear = Integer.parseInt(examPeriod.replaceAll("\\D+", "").substring(0, 4));
        int currentYear = (courseYear - registrationYear) + 1;
        return ((currentYear * 2) - coursePeriodNumber);
    }
}
