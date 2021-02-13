package com.unistudents.api.parser;

import com.unistudents.api.common.StringHelper;
import com.unistudents.api.model.Info;
import com.unistudents.api.model.Student;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
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
            int semester = getCurrentSemester(gradesPage, registrationYear);

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

    // Get current student's semester, based on the last subject declaration.
    private int getCurrentSemester(Document gradesPage, int registrationYear) {
        Elements lastSubject = gradesPage.select("#tab_2 > table > tbody > tr").last().select("td");
        String lastSubjectRegistration = lastSubject.last().text();
        return getSemesterFromExamPeriod(lastSubjectRegistration, registrationYear);
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
