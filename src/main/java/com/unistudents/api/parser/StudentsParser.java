package com.unistudents.api.parser;

import com.unistudents.api.model.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class StudentsParser {

    private Document infoPage;
    private Document gradesPage;
    private GradeResults results;

    public StudentsParser() {
    }

    public StudentsParser(Document infoPage, Document gradesPage) {
        this.infoPage = infoPage;
        this.gradesPage = gradesPage;
        results = new GradeResults();
        this.setStudentInfo();
        this.setGrades();
    }

    public Info parseInfoPage(Document infoPage) {
        Elements table = infoPage.getElementsByAttributeValue("cellpadding", "4");

        Info info = new Info();

        int counter = 0;
        for (Element element : table.select("tr")) {
            counter++;

            // get aem
            switch (counter) {
                case 6:
                    info.setLastName(element.select("td").get(1).text());
                    break;
                case 7:
                    info.setFirstName(element.select("td").get(1).text());
                    break;
                case 8:
                    info.setAem(element.select("td").get(1).text());
                case 9:
                    info.setDeparture(element.select("td").get(1).text());
                    break;
                case 10:
                    info.setSemester(element.select("td").get(1).text());
                    break;
                case 11:
                    info.setRegistrationYear(element.select("td").get(1).text());
            }
        }
        return info;
    }

    public Grades parseGradesPage(Document gradesPage) {
        Element elements = gradesPage.getElementById("mainTable");
        Elements table = elements.getElementsByAttributeValue("cellspacing", "0");

        Grades results = new Grades();
        Semester semesterObj = null;
        Course courseObj = null;

        for (Element element : table.select("tr")) {

            // get new semester
            Elements semester = element.select("td.groupheader");
            if (semester != null) {
                if (!semester.text().equals("")) {
                    semesterObj = new Semester();

                    // set semester id
                    int id = Integer.parseInt(semester.text().substring(semester.text().length() - 1));
                    semesterObj.setId(id);
                }
            }

            // get courses
            Elements course = element.getElementsByAttributeValue("bgcolor", "#fafafa");
            if (course != null) {
                if (!course.text().equals("")) {
                    int counter = 0;
                    for (Element courseElement : course.select("td")) {
                        counter++;

                        // get course id & name
                        Elements courseName = courseElement.getElementsByAttributeValue("colspan", "2");
                        if (courseName != null) {
                            if (!courseName.text().equals("")) {
                                courseObj = new Course();
                                String name = courseName.text();
                                courseObj.setName(name.substring(name.indexOf(") ") + 2));
                                courseObj.setId(name.substring(name.indexOf("(")+1,name.indexOf(")")));
                            }
                        }

                        if (counter == 3) {
                            courseObj.setType(courseElement.text());
                        }
                        else if (counter == 7) {
                            courseObj.setGrade(courseElement.text());
                        }
                        else if (counter == 8) {
                            courseObj.setExamPeriod(courseElement.text());
                        }
                    }
                    semesterObj.getCourses().add(courseObj);
                }
            }

            // get final info & add semester obj
            Elements finalInfo = element.select("tr.subHeaderBack");
            if (finalInfo != null) {
                if (!finalInfo.text().equals("")) {


                    for (Element finalInfoEl : finalInfo) {

                        // get total passed courses
                        Elements elPassesCourses = finalInfoEl.getElementsByAttributeValue("colspan", "3");
                        if (elPassesCourses != null) {
                            if (results.getSemesters().contains(semesterObj)) {
                                results.setTotalPassedCourses(Integer.parseInt(elPassesCourses.text().substring(elPassesCourses.text().length() - 2)));
                            } else {
                                semesterObj.setPassedCourses(Integer.parseInt(elPassesCourses.text().substring(elPassesCourses.text().length() - 1)));
                            }
                        }

                        // get semester avg
                        Elements tableCell = finalInfoEl.getElementsByAttributeValue("colspan", "10");
                        if (tableCell != null) {
                            int counter = 0;
                            for (Element el : tableCell.select(".error")) {
                                counter++;
                                if (counter == 1) {
                                    if (results.getSemesters().contains(semesterObj)) {
                                        results.setTotalAverageGrade(el.text().replace("-",""));
                                    }
                                    else {
                                        semesterObj.setGradeAverage(el.text());
                                    }
                                }
                                else if (counter == 4) {
                                    if (results.getSemesters().contains(semesterObj)) {
                                        results.setTotalEcts(Integer.parseInt(el.text()));
                                    }
                                    else {
                                        semesterObj.setEcts(Integer.parseInt(el.text()));
                                    }
                                }
                            }
                        }
                    }

                    // add semesterObj to resultsObj
                    if (!results.getSemesters().contains(semesterObj))
                        results.getSemesters().add(semesterObj);
                }
            }
        }
        return results;
    }

    private void setStudentInfo() {

//        Elements table = infoPage.getElementsByAttributeValue("cellpadding", "4");
//
//        StudentObj studentObjInfo = new StudentObj();
//
//        int counter = 0;
//        for (Element element : table.select("tr")) {
//            counter++;
//
//            // get aem
//            switch (counter) {
//                case 6:
//                    studentObjInfo.setLastName(element.select("td").get(1).text());
//                    break;
//                case 7:
//                    studentObjInfo.setFirstName(element.select("td").get(1).text());
//                    break;
//                case 8:
//                    studentObjInfo.setAem(element.select("td").get(1).text());
//                case 9:
//                    studentObjInfo.setDeparture(element.select("td").get(1).text());
//                    break;
//                case 10:
//                    studentObjInfo.setSemester(element.select("td").get(1).text());
//                    break;
//                case 11:
//                    studentObjInfo.setRegistrationYear(element.select("td").get(1).text());
//            }
//        }
//        this.results.setStudentObj(studentObjInfo);
    }

    private void setGrades() {

//        Element elements = gradesPage.getElementById("mainTable");
//        Elements table = elements.getElementsByAttributeValue("cellspacing", "0");
//
//        Semester semesterObj = null;
//        Course courseObj = null;
//
//        for (Element element : table.select("tr")) {
//
//            // get new semester
//            Elements semester = element.select("td.groupheader");
//            if (semester != null) {
//                if (!semester.text().equals("")) {
//                    semesterObj = new Semester();
//
//                    // set semester id
//                    int id = Integer.parseInt(semester.text().substring(semester.text().length() - 1));
//                    semesterObj.setId(id);
//                }
//            }
//
//            // get courses
//            Elements course = element.getElementsByAttributeValue("bgcolor", "#fafafa");
//            if (course != null) {
//                if (!course.text().equals("")) {
//                    int counter = 0;
//                    for (Element courseElement : course.select("td")) {
//                        counter++;
//
//                        // get course id & name
//                        Elements courseName = courseElement.getElementsByAttributeValue("colspan", "2");
//                        if (courseName != null) {
//                            if (!courseName.text().equals("")) {
//                                courseObj = new Course();
//                                String name = courseName.text();
//                                courseObj.setName(name.substring(name.indexOf(") ") + 2));
//                                courseObj.setId(name.substring(name.indexOf("(")+1,name.indexOf(")")));
//                            }
//                        }
//
//                        if (counter == 3) {
//                            courseObj.setType(courseElement.text());
//                        }
//                        else if (counter == 7) {
//                            courseObj.setGrade(courseElement.text());
//                        }
//                        else if (counter == 8) {
//                            courseObj.setExamPeriod(courseElement.text());
//                        }
//                    }
//                    semesterObj.getCourses().add(courseObj);
//                }
//            }
//
//            // get final info & add semester obj
//            Elements finalInfo = element.select("tr.subHeaderBack");
//            if (finalInfo != null) {
//                if (!finalInfo.text().equals("")) {
//
//
//                    for (Element finalInfoEl : finalInfo) {
//
//                        // get total passed courses
//                        Elements elPassesCourses = finalInfoEl.getElementsByAttributeValue("colspan", "3");
//                        if (elPassesCourses != null) {
//                            if (results.getSemesters().contains(semesterObj)) {
//                                results.setTotalPassedCourses(Integer.parseInt(elPassesCourses.text().substring(elPassesCourses.text().length() - 2)));
//                            } else {
//                                semesterObj.setPassedCourses(Integer.parseInt(elPassesCourses.text().substring(elPassesCourses.text().length() - 1)));
//                            }
//                        }
//
//                        // get semester avg
//                        Elements tableCell = finalInfoEl.getElementsByAttributeValue("colspan", "10");
//                        if (tableCell != null) {
//                            int counter = 0;
//                            for (Element el : tableCell.select(".error")) {
//                                counter++;
//                                if (counter == 1) {
//                                    if (results.getSemesters().contains(semesterObj)) {
//                                        results.setTotalAverageGrade(el.text().replace("-",""));
//                                    }
//                                    else {
//                                        semesterObj.setGradeAverage(el.text());
//                                    }
//                                }
//                                else if (counter == 4) {
//                                    if (results.getSemesters().contains(semesterObj)) {
//                                        results.setTotalEcts(Integer.parseInt(el.text()));
//                                    }
//                                    else {
//                                        semesterObj.setEcts(Integer.parseInt(el.text()));
//                                    }
//                                }
//                            }
//                        }
//                    }
//
//                    // add semesterObj to resultsObj
//                    if (!results.getSemesters().contains(semesterObj))
//                        results.getSemesters().add(semesterObj);
//                }
//            }
//        }
    }

    public GradeResults getResults() {
        return this.results;
    }
}
