package com.studyflow.app.util;

import com.studyflow.app.exception.ArgumentNotValidException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ValidationUtil {
    private final Validator validator;

    public ValidationUtil() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    public <T> void validate(T object) throws ArgumentNotValidException {
        Set<ConstraintViolation<T>> violations = validator.validate(object);

        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<T> violation : violations) {
                sb.append("-").append(violation.getMessage());
            }

            throw new ArgumentNotValidException(sb.toString());
        }
    }

    public void validatePassword(String password) throws ArgumentNotValidException{
       password = password.trim();
       if (password.length() > 20 || password.length() < 6){
           throw new ArgumentNotValidException("password");
       }
    }

    public void validateEmail(String email) throws ArgumentNotValidException{
        String regex = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        Pattern emailPattern = Pattern.compile(regex);
        if (!emailPattern.matcher(email).matches() || email.length() > 127){
            throw new ArgumentNotValidException("email");
        }
    }
}
