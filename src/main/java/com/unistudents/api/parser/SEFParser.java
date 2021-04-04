package com.unistudents.api.parser;

import com.unistudents.api.common.StringHelper;
import com.unistudents.api.model.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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

        String totalECTS = passedSubjects.last().select("td").last().text();
        String totalPassedCourses = passedSubjects.get(passedSubjects.size() - 3).select("td").last().text().split(" ")[0].trim();

        grades.setTotalAverageGrade("-");
        grades.setTotalEcts(totalECTS);
        grades.setTotalPassedCourses(totalPassedCourses);

        for (Semester semester : declaredCourses) {
            int semesterECTS = 0;
            int semesterPassedCourses = 0;
            for (int i = 0; i < semester.getCourses().size(); i++) {
                Course declaredCourse = semester.getCourses().get(i);
                for (Element passedCourse : passedSubjects.subList(1, passedSubjects.size() - 3)) {
                    Elements passedCourseInfo = passedCourse.select("td");
                    String passedCourseId = passedCourseInfo.get(0).text();
                    boolean isSuccess = passedCourseInfo.get(11).text().equals("Επιτυχία");
                    if (declaredCourse.getId().equals(passedCourseId) && isSuccess) {
                        float passedCourseGrade = Float.parseFloat(passedCourseInfo.get(10).text());
                        boolean isCalculated = passedCourseInfo.get(7).text().equals("Ναι");
                        if (passedCourseGrade >= 5 && passedCourseGrade <= 10 && isCalculated) {
                            int passedCourseECTS = Integer.parseInt(passedCourseInfo.get(4).text());
                            semesterECTS += passedCourseECTS;
                            semesterPassedCourses++;
                        }
                        declaredCourse.setGrade(String.valueOf(passedCourseGrade));
                    }
                }
            }

            semester.setPassedCourses(semesterPassedCourses);
            semester.setEcts(String.valueOf(semesterECTS));
        }

        grades.setSemesters(declaredCourses);
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
        HashMap<String, String> mathCourses = initMathCourses();
        HashMap<String, String> saxmCourses = initSAXMCourses();

        for (int i = declaredSubjectsDOM.size() - 1; i >= 0; i--) {
            Elements course = declaredSubjectsDOM.get(i).select("td");
            String courseId = course.get(0).text();
            String courseExamPeriod = course.get(8).text();

            String semesterId;
            String courseNameSuffix = "";
            if ((isSemesterValid(course.get(2).text()))) {
                if (mathCourses.get(courseId) != null) {
                    semesterId = mathCourses.get(courseId);
                } else if (saxmCourses.get(courseId) != null) {
                    semesterId = saxmCourses.get(courseId);
                ***REMOVED***
                    semesterId = String.valueOf(Integer.parseInt(course.get(2).text()));
                }
            ***REMOVED***
                if (!isSemesterContainsNumbers(course.get(2).text())) {
                    if (mathCourses.get(courseId) != null) {
                        semesterId = mathCourses.get(courseId);
                    } else if (saxmCourses.get(courseId) != null) {
                        semesterId = saxmCourses.get(courseId);
                    ***REMOVED***
                        semesterId = String.valueOf(getSemesterFromExamPeriod(courseExamPeriod, registrationYear));
                    }
                    courseNameSuffix = course.get(2).text();
                ***REMOVED***
                    if (mathCourses.get(courseId) != null) {
                        semesterId = mathCourses.get(courseId);
                    } else if (saxmCourses.get(courseId) != null) {
                        semesterId = saxmCourses.get(courseId);
                    ***REMOVED***
                        semesterId = String.valueOf(Integer.parseInt(course.get(2).text().split(",")[0]));
                    }
                }
            }

            if (!insertedCourses.contains(courseId)) {
                insertedCourses.add(courseId);

                String courseName;
                if (courseNameSuffix.equals("")) {
                    courseName = course.get(1).text();
                ***REMOVED***
                    courseName = course.get(1).text() + " (" + StringHelper.removeTones(courseNameSuffix.toUpperCase()) + ")";
                }

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

    // Check if semester value is in "01,02" format.
    private boolean isSemesterContainsNumbers(String semester) {
        return semester.matches("\\d\\d,\\d\\d");
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
        ***REMOVED***
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

    private HashMap<String, String> initMathCourses() {
        HashMap<String, String> mathCourses = new HashMap<String, String>();

        // Semester: 1
        mathCourses.put("311-0012", "1");
        mathCourses.put("311-0543", "1");
        mathCourses.put("311-3900", "1");
        mathCourses.put("311-3950", "1");

        // Semester: 2
        mathCourses.put("311-0039", "2");
        mathCourses.put("311-0045", "2");
        mathCourses.put("311-0072", "2");
        mathCourses.put("311-1603", "2");

        // Semester: 3
        mathCourses.put("311-0086", "3");
        mathCourses.put("311-0194", "3");
        mathCourses.put("311-0551", "3");
        mathCourses.put("311-1653", "3");
        mathCourses.put("311-0106", "3");
        mathCourses.put("311-0186", "3");
        mathCourses.put("311-3351", "3");

        // Semester: 4
        mathCourses.put("311-0025", "4");
        mathCourses.put("311-0134", "4");
        mathCourses.put("311-0571", "4");
        mathCourses.put("311-1703", "4");
        mathCourses.put("311-0117", "4");
        mathCourses.put("311-0206", "4");
        mathCourses.put("311-0334", "4");
        mathCourses.put("311-3500", "4");

        // Semester: 5
        mathCourses.put("311-0297", "5");
        mathCourses.put("311-0562", "5");
        mathCourses.put("311-0824", "5");
        mathCourses.put("311-1051", "5");
        mathCourses.put("311-2304", "5");
        mathCourses.put("311-0926", "5");
        mathCourses.put("311-2453", "5");
        mathCourses.put("311-2653", "5");
        mathCourses.put("311-3800", "5");

        // Semester: 6
        mathCourses.put("311-0163", "6");
        mathCourses.put("311-0257", "6");
        mathCourses.put("311-3600", "6");
        mathCourses.put("311-0266", "6");
        mathCourses.put("311-0437", "6");
        mathCourses.put("311-0506", "6");
        mathCourses.put("311-1452", "6");
        mathCourses.put("311-2003", "6");
        mathCourses.put("311-2851", "6");
        mathCourses.put("311-0515", "6");
        mathCourses.put("311-0983", "6");

        // Semester: 7
        mathCourses.put("311-0224", "7");
        mathCourses.put("311-0239", "7");
        mathCourses.put("311-0832", "7");
        mathCourses.put("311-1953", "7");
        mathCourses.put("311-2353", "7");
        mathCourses.put("11-3001", "7");
        mathCourses.put("11-3002", "7");
        mathCourses.put("11-3004", "7");
        mathCourses.put("311-3251", "7");
        mathCourses.put("311-3400", "7");
        mathCourses.put("311-3551", "7");
        mathCourses.put("311-3850", "7");
        mathCourses.put("311-0359", "7");
        mathCourses.put("311-0453", "7");
        mathCourses.put("311-2554", "7");
        mathCourses.put("311-2564", "7");
        mathCourses.put("311-2752", "7");
        mathCourses.put("311-3650", "7");

        // Semester: 8
        mathCourses.put("311-0246", "8");
        mathCourses.put("311-0308", "8");
        mathCourses.put("311-0445", "8");
        mathCourses.put("311-1004", "8");
        mathCourses.put("311-2701", "8");
        mathCourses.put("311-3001", "8");
        mathCourses.put("311-3002", "8");
        mathCourses.put("311-3004", "8");
        mathCourses.put("311-3101", "8");
        mathCourses.put("311-1156", "8");
        mathCourses.put("311-1252", "8");
        mathCourses.put("311-1406", "8");
        mathCourses.put("311-2403", "8");
        mathCourses.put("311-2505", "8");
        mathCourses.put("311-2573", "8");
        mathCourses.put("311-2582", "8");
        mathCourses.put("311-2602", "8");

        return mathCourses;
    }

    private HashMap<String, String> initSAXMCourses() {
        HashMap<String, String> saxmCourses = new HashMap<String, String>();

        // Semester: 1
        saxmCourses.put("331-1006", "1");
        saxmCourses.put("331-1172", "1");
        saxmCourses.put("331-1108", "1");
        saxmCourses.put("331-2107", "1");
        saxmCourses.put("331-0462", "1");

        // Semester: 2
        saxmCourses.put("331-2006", "2");
        saxmCourses.put("331-1164", "2");
        saxmCourses.put("331-2980", "2");
        saxmCourses.put("331-1207", "2");
        saxmCourses.put("331-1056", "2");
        saxmCourses.put("331-0510", "2");

        // Semester: 3
        saxmCourses.put("331-2058", "3");
        saxmCourses.put("331-2808", "3");
        saxmCourses.put("331-3970", "3");
        saxmCourses.put("331-2256", "3");
        saxmCourses.put("331-5065", "3");
        saxmCourses.put("331-5026", "3");
        saxmCourses.put("331-4755", "3");
        saxmCourses.put("331-4257", "3");
        saxmCourses.put("331-0560", "3");

        // Semester: 4
        saxmCourses.put("331-2160", "4");
        saxmCourses.put("331-2309", "4");
        saxmCourses.put("331-2408", "4");
        saxmCourses.put("331-2207", "4");
        saxmCourses.put("331-2658", "4");
        saxmCourses.put("331-4925", "4");
        saxmCourses.put("331-5056", "4");
        saxmCourses.put("331-4853", "4");

        // Semester: 5
        saxmCourses.put("331-2457", "5");
        saxmCourses.put("331-3009", "5");
        saxmCourses.put("331-3109", "5");
        saxmCourses.put("331-4057", "5");
        saxmCourses.put("331-5007", "5");
        saxmCourses.put("331-3257", "5");
        saxmCourses.put("331-5084", "5");
        saxmCourses.put("331-3957", "5");

        // Semester: 6
        saxmCourses.put("331-2711", "6");
        saxmCourses.put("331-6006", "6");
        saxmCourses.put("331-3709", "6");
        saxmCourses.put("331-3406", "6");
        saxmCourses.put("331-2757", "6");
        saxmCourses.put("331-3554", "6");
        saxmCourses.put("331-4207", "6");
        saxmCourses.put("331-3754", "6");
        saxmCourses.put("331-3508", "6");
        saxmCourses.put("331-4357", "6");
        saxmCourses.put("331-4306", "6");

        // Semester: 7
        saxmCourses.put("331-3309", "7");
        saxmCourses.put("331-4707", "7");
        saxmCourses.put("331-4157", "7");
        saxmCourses.put("331-3808", "7");
        saxmCourses.put("331-4107", "7");
        saxmCourses.put("331-7104", "7");
        saxmCourses.put("331-5102", "7");
        saxmCourses.put("331-4007", "7");
        saxmCourses.put("331-5092", "7");
        saxmCourses.put("331-3657", "7");
        saxmCourses.put("331-9302", "7");
        saxmCourses.put("331-9752", "7");
        saxmCourses.put("331-9355", "7");
        saxmCourses.put("331-9106", "7");
        saxmCourses.put("331-9703", "7");
        saxmCourses.put("331-4656", "7");
        saxmCourses.put("331-9028", "7");
        saxmCourses.put("331-7088", "7");

        // Semester: 8
        saxmCourses.put("331-4457", "8");
        saxmCourses.put("331-9205", "8");
        saxmCourses.put("331-9920", "8");
        saxmCourses.put("331-9601", "8");
        saxmCourses.put("331-3607", "8");
        saxmCourses.put("331-4965", "8");
        saxmCourses.put("331-9930", "8");
        saxmCourses.put("331-8143", "8");
        saxmCourses.put("331-3156", "8");
        saxmCourses.put("331-4408", "8");
        saxmCourses.put("331-4714", "8");
        saxmCourses.put("331-9402", "8");
        saxmCourses.put("331-4943", "8");
        saxmCourses.put("331-4557", "8");
        saxmCourses.put("331-6104", "8");
        saxmCourses.put("331-4992", "8");
        saxmCourses.put("331-9652", "8");
        saxmCourses.put("331-9154", "8");
        saxmCourses.put("331-9054", "8");
        saxmCourses.put("331-9900", "8");
        saxmCourses.put("331-9027", "8");
        saxmCourses.put("331-4611", "8");
        saxmCourses.put("331-9802", "8");

        return saxmCourses;
    }
}
