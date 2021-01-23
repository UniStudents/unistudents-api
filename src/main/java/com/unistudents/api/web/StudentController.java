package com.unistudents.api.web;

import com.unistudents.api.model.LoginForm;
import com.unistudents.api.service.MockService;
import com.unistudents.api.service.ScrapeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@EnableCaching
public class StudentController {

    @Autowired
    private ScrapeService scrapeService;

    @Autowired
    private MockService mockService;

    @RequestMapping(value = {"/student/{university}", "/student/{university}/{system}"}, method = RequestMethod.POST)
    public ResponseEntity getStudent(
            @PathVariable("university") String university,
            @PathVariable(required = false) String system,
            @RequestBody LoginForm loginForm) {
        return scrapeService.getStudent(university.toUpperCase(), system != null ? system.toUpperCase() : null, loginForm);
    }

    @RequestMapping(value = {"/mock/student/{university}", "/mock/student/{university}/{system}"}, method = RequestMethod.POST)
    public ResponseEntity getStudentMock(
            @PathVariable(required = false) String university,
            @PathVariable(required = false) String system) {
        return mockService.getStudent(university != null ? university.toUpperCase() : null, system != null ? system.toUpperCase() : null);
    }
}
