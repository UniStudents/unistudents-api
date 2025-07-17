package com.unistudents.api.common;

import gr.unistudents.services.student.models.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class ProgressUtils {

    public static Progress populate(Progress progress) {
        int passedCourses = 0;
        int failedCourses = 0;
        double ects = 0.0;
        double credits = 0.0;
        int calculatedCourses = 0;
        double avgGrade = 0.0;
        double weightedAvgGrade = 0.0;

        for (Semester semester : progress.semesters) {
            int _passedCourses = 0;
            int _failedCourses = 0;
            double _ects = 0.0;
            double _credits = 0.0;
            int _calculatedCourses = 0;
            double _avgGrade = 0.0;
            double _weightedAvgGrade = 0.0;

            for (Course course : semester.courses) {
                ExamGrade latestExamGrade = course.latestExamGrade;

                if (latestExamGrade != null && Boolean.TRUE.equals(latestExamGrade.isPassed)) {
                    passedCourses++;
                    _passedCourses++;

                    // If null or true (isCalculated != false)
                    if (!Boolean.FALSE.equals(course.isCalculated) && latestExamGrade.grade != null) {
                        calculatedCourses++;
                        _calculatedCourses++;
                        avgGrade += latestExamGrade.grade;
                        _avgGrade += latestExamGrade.grade;

                        if (course.weight != null) {
                            weightedAvgGrade += latestExamGrade.grade * course.weight;
                            _weightedAvgGrade += latestExamGrade.grade * course.weight;
                        }
                    }
                } else if (latestExamGrade != null && Boolean.FALSE.equals(latestExamGrade.isPassed)) {
                    failedCourses++;
                    _failedCourses++;
                }

                ects += course.ects != null ? course.ects : 0;
                _ects += course.ects != null ? course.ects : 0;
                credits += course.credits != null ? course.credits : 0;
                _credits += course.credits != null ? course.credits : 0;

                // Check display period
                List<ExamGrade> examGrades = new ArrayList<>();
                if (course.latestExamGrade != null) {
                    examGrades.add(course.latestExamGrade);
                }
                if (course.examGradeHistory != null) {
                    examGrades.addAll(course.examGradeHistory);
                }

                for (ExamGrade exam : examGrades) {
                    if (exam.academicYear != null && exam.examPeriod != null && course.isExempted != null) {
                        continue;
                    }
                    if (exam.displayPeriod == null) {
                        continue;
                    }

                    DisplayPeriodResult result = ProgressUtils.extractDisplayPeriod(exam.displayPeriod);

                    if (course.isExempted == null && result.isExempted != null) {
                        course.isExempted = result.isExempted;
                    }
                    if (exam.examPeriod == null && result.examPeriod != null) {
                        exam.examPeriod = result.examPeriod;
                    }
                    if (exam.academicYear == null && result.academicYear != null) {
                        exam.academicYear = result.academicYear;
                    }
                }
            }

            if (semester.passedCourses == null) semester.passedCourses = _passedCourses;
            if (semester.failedCourses == null) semester.failedCourses =  _failedCourses;
            if (semester.ects == null) semester.ects = _ects;
            if (semester.credits == null) semester.credits = _credits;
            if (semester.averageGrade == null) {
                semester.averageGrade = ProgressUtils.formatDF2(_avgGrade / _calculatedCourses);
            }
            if (semester.weightedAverageGrade == null) {
                semester.weightedAverageGrade = _weightedAvgGrade != 0 ? ProgressUtils.formatDF2(_weightedAvgGrade / _calculatedCourses) : null;
            }
        }

        if (progress.passedCourses == null) progress.passedCourses = passedCourses;
        if (progress.failedCourses == null) progress.failedCourses = failedCourses;
        if (progress.ects == null) progress.ects = ects;
        if (progress.credits == null) progress.credits = credits;
        if (progress.averageGrade == null) {
            progress.averageGrade = ProgressUtils.formatDF2(avgGrade / calculatedCourses);
        }
        if (progress.weightedAverageGrade == null) {
            progress.weightedAverageGrade = weightedAvgGrade != 0 ? ProgressUtils.formatDF2(weightedAvgGrade / calculatedCourses) : null;
        }

        return progress;
    }

    public static DisplayPeriodResult extractDisplayPeriod(String dp) {
        ExamPeriod examPeriod = extractPeriod(dp);
        String academicYear = extractAcademicYear(dp, examPeriod);
        Boolean isExempted = extractIsExempted(dp);

        return new DisplayPeriodResult(academicYear, examPeriod, isExempted);
    }

    private static ExamPeriod extractPeriod(String dp) {
        dp = dp.trim();

        List<String> winter = Arrays.asList("ΙΑΝΟΥΑΡΙΟΣ", "ΦΕΒ", "XEIM", "Δεκ", "Φεβ", "ΦΕΒΡ",
                "FEBRUARY", "Jan", "(Χ)", "-Χ)", "(Χ/", "(Χ", "χειμερινού", "ΙΑΝ");
        List<String> spring = Arrays.asList("IΟΥΝΙΟΣ", "Ιούνιος", "Απρ", "Ιουλ", "Μαρ", "ΙΟΥΛΙΟΣ",
                "ΙΟΥΝ", "ΑΠΡΙΛΙΟΣ", "ΙΟΥΛΙΟΥ", "ΜΑΪΟΣ", "ΜΑΡΤΙΟΣ", "Jun", "ΕΑΡ", "ΕΑΡΙΝΗ", "(Ε)",
                "-Ε)", "(Ε/", "(Ε", "θερινού", "ΘΕΡΙΝΗ", "ΙΟΥΛ", "ΙΟΥΝΙΟΣ", "Μαι");
        List<String> reexam = Arrays.asList("ΣΕΠΤΕΜΒΡΙΟΥ", "ΣΕΠΤ", "Νοε", "Οκτ", "ΕΠΤΕΜΒΡΙΟΣ",
                "(Σ)", "-Σ)", "(Σ/", "(Σ", "ΣΕΠ", "ΟΚΤ");
        List<String> extra = Arrays.asList("ΕΜΒΟΛΙΜΗ", "Εμβόλιμη", "ΕΜΒΟΛ", "ΕΜΒ", "Επαν.",
                "Εμβ.", "ΕΜΒ.", "ΑΝΑΒ", "ΕΠΑΝ", "ΠΡΟΣΘ");

        if (ProgressUtils.inLike(dp, winter, true)) {
            return ProgressUtils.inLike(dp, extra, true) ? ExamPeriod.EXTRA_WINTER : ExamPeriod.WINTER;
        } else if (ProgressUtils.inLike(dp, spring, true)) {
            return ProgressUtils.inLike(dp, extra, true) ? ExamPeriod.EXTRA_SPRING : ExamPeriod.SPRING;
        } else if (ProgressUtils.inLike(dp, reexam, true)) {
            return ExamPeriod.RE_EXAM;
        }

        Integer num = null;
        Pattern digitsOnly = Pattern.compile("^\\d*$");
        Pattern digitsAndSpaces = Pattern.compile("^[\\d\\s]*$");

        if (digitsOnly.matcher(dp).matches()) {
            // Concatenated year and month
            if (dp.length() == 5 || dp.length() == 6) {
                // Starts with year 20XX or 19XX
                if (isPrefixYear(dp)) {
                    // Year first
                    String year = dp.substring(0, 4);
                    String month = dp.substring(4);
                    num = Integer.parseInt(month);
                } else {
                    // Month first
                    String month = dp.substring(0, dp.length() - 4);
                    String year = dp.substring(dp.length() - 4);
                    num = Integer.parseInt(month);
                }
            }
        } else if (digitsAndSpaces.matcher(dp).matches()) {
            String normalized = ProgressUtils.normalizeSpace(dp);
            if (normalized != null) {
                String[] parts = normalized.split(" ");
                String year = parts[0];
                String month = parts.length > 1 ? parts[1] : null;

                if (year.length() != 4 && year.length() == 2 && !isPrefixYear(year)) {
                    month = year;
                }

                if (month != null) {
                    try {
                        num = Integer.parseInt(month);
                    } catch (NumberFormatException e) {
                        // Ignore parsing errors
                    }
                }
            }
        }

        if (num != null) {
            if (num >= 1 && num <= 4) {
                return ExamPeriod.WINTER;
            } else if (num >= 5 && num <= 8) {
                return ExamPeriod.SPRING;
            } else if (num >= 8 && num <= 11) {
                return ExamPeriod.RE_EXAM;
            }
        }

        return null;
    }

    private static boolean isPrefixYear(String dp) {
        if (dp == null || dp.length() < 2) return false;
        return (dp.charAt(0) == '2' && dp.charAt(1) == '0') ||
                (dp.charAt(0) == '1' && dp.charAt(1) == '9');
    }

    private static Boolean extractIsExempted(String dp) {
        List<String> exemptionTerms = Arrays.asList("Απαλλαγή");
        return ProgressUtils.inLike(dp, exemptionTerms, true);
    }

    private static String extractAcademicYear(String dp, ExamPeriod examPeriod) {
        String noNoise = dp.replaceAll("[^0-9\\-]", " ");
        String normalized = ProgressUtils.normalizeSpace(noNoise);
        if (normalized == null) return null;

        noNoise = normalized.trim();
        String first = null;
        String second = null;

        if (noNoise.contains("-")) {
            String[] parts = noNoise.split("-");
            first = parts[0];
            if (parts.length > 1) {
                second = parts[1];
            }
        } else if (noNoise.contains(" ")) {
            String[] spl = noNoise.split(" ");

            if (spl.length > 2) {
                // Remove all not needed parts
                for (int i = 0; i < spl.length; i++) {
                    if (!isPrefixYear(spl[i])) {
                        spl[i] = null;
                    }
                }

                // Filter out nulls
                spl = Arrays.stream(spl)
                        .filter(it -> it != null)
                        .toArray(String[]::new);
            }

            first = spl.length > 0 ? spl[0] : null;
            second = spl.length > 1 ? spl[1] : null;
        }

        if (examPeriod == ExamPeriod.WINTER && first != null) {
            return first.trim();
        }
        if (examPeriod == ExamPeriod.SPRING && second != null) {
            return second.trim();
        }
        if (examPeriod == ExamPeriod.RE_EXAM && second != null) {
            return second.trim();
        }

        return first;
    }

    // Result class to hold the return values
    public static class DisplayPeriodResult {
        private final String academicYear;
        private final ExamPeriod examPeriod;
        private final Boolean isExempted;

        public DisplayPeriodResult(String academicYear, ExamPeriod examPeriod, Boolean isExempted) {
            this.academicYear = academicYear;
            this.examPeriod = examPeriod;
            this.isExempted = isExempted;
        }
    }

    public static Double formatDF2(Double x) {
        if (x == null || Double.isNaN(x)) {
            return null;
        }
        return Math.round(x * 100.0) / 100.0;
    }

    public static String normalizeSpace(String str) {
        if (str == null) {
            return null;
        }

        str = str.trim();

        if (str.length() > 2) {
            StringBuilder b = new StringBuilder();

            for (int i = 0; i < str.length(); i++) {
                if (str.charAt(i) == ' ') {
                    if (i > 0 && str.charAt(i - 1) != ' ') {
                        b.append(' ');
                    }
                } else {
                    b.append(str.charAt(i));
                }
            }

            return b.toString();
        } else {
            return str;
        }
    }

    public static boolean inLike(String searchString, List<String> stringArray, boolean ignoreCase) {
        if (searchString == null || stringArray == null) {
            return false;
        }

        if (ignoreCase) {
            String lowerSearchString = searchString.toLowerCase();
            return stringArray.stream()
                    .anyMatch(item -> item != null && lowerSearchString.contains(item.toLowerCase()));
        }

        return stringArray.stream()
                .anyMatch(item -> item != null && searchString.contains(item));
    }
}