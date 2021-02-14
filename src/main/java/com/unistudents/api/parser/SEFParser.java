package com.unistudents.api.parser;

import com.unistudents.api.common.StringHelper;
import com.unistudents.api.model.Info;
import com.unistudents.api.model.Student;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

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

    public Student parseInfoAndGradesPages(Document infoPage, Document gradesPage) {
        Student student = new Student();

        try {
            Info info = parseInfoPage(infoPage, gradesPage);

            if (info == null) {
                return null;
            }

            student.setInfo(info);

            return student;
        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage(), e);
            return null;
        }
    }

    // Get registration year from the first subject declaration.
    private int getRegistrationYear(Document gradesPage) {
        Elements firstSubject = gradesPage.select("#tab_2 > table > tbody > tr").get(0).select("td");
        String firstSubjectRegistration = firstSubject.last().text();
        return Integer.parseInt(firstSubjectRegistration.replaceAll("\\D+", "").substring(0, 4));
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
        String subjectPeriod = examPeriod.split(" ")[0];
        int subjectPeriodNumber = ((subjectPeriod.equals("Χειμερινό")) ? 1 : 0);
        int subjectYear = Integer.parseInt(examPeriod.replaceAll("\\D+", "").substring(0, 4));
        int currentYear = (subjectYear - registrationYear) + 1;
        return ((currentYear * 2) - subjectPeriodNumber);
    }
}
