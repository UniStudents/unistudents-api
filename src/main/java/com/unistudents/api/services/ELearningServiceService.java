package com.unistudents.api.services;

import com.unistudents.api.components.LoginForm;
import gr.unistudents.services.elearning.ELearningService;
import gr.unistudents.services.elearning.exceptions.InvalidCredentialsException;
import gr.unistudents.services.elearning.exceptions.NotAuthorizedException;
import gr.unistudents.services.elearning.components.Options;
import gr.unistudents.services.elearning.exceptions.NotReachableException;
import gr.unistudents.services.elearning.exceptions.PlatformException;
import gr.unistudents.services.elearning.models.ELearningResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class ELearningServiceService {

    private ResponseEntity<Object> guest(String university, LoginForm loginForm) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
    public ResponseEntity<Object> get(String university, LoginForm loginForm) {
        if(university.toLowerCase(Locale.ROOT).equals("guest"))
            return guest(university, loginForm);

        Options options = new Options();
        options.university = university.toLowerCase(Locale.ROOT);
        options.username = loginForm.getUsername();
        options.password = loginForm.getPassword();
        options.cookies = loginForm.getCookies();

        ELearningService result = new ELearningService(options);
        try {
            ELearningResponse response = result.get();
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (NotAuthorizedException e) {
            if (e.getException() != null) e.getException().printStackTrace();
            else e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        } catch (InvalidCredentialsException e) {
            if (e.getException() != null) e.getException().printStackTrace();
            else e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        } catch (NotReachableException e) {
            if (e.getException() != null) e.getException().printStackTrace();
            else e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        } catch (PlatformException e) {
            if (e.getException() != null) e.getException().printStackTrace();
            else e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}