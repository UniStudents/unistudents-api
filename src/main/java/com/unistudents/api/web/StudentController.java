package com.unistudents.api.web;

import com.unistudents.api.model.LoginForm;
import com.unistudents.api.model.Student;
import com.unistudents.api.service.ScrapeService;
import com.unistudents.api.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private ScrapeService scrapeService;

    @PostMapping("/student/{university}")
    public ResponseEntity getStudent(@PathVariable("university") String university, @RequestBody LoginForm loginForm) {
        return scrapeService.getStudent(university.toUpperCase(), loginForm.getUsername(), loginForm.getPassword());
    }

    /*
     *  Support older versions of UniStudents app
     */
    @PostMapping("/student")
    public ResponseEntity getStudentUNIPI(@RequestBody LoginForm loginForm) {
        Student student = studentService.getStudent(loginForm.getUsername(), loginForm.getPassword());

        if (student == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        else {
            return new ResponseEntity<>(student, HttpStatus.OK);
        }
    }
}
