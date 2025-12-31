package com.studyflow.app.util;

import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

@Component
public class PasswordUtil{
    public String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public Boolean checkPassword(String password, String hash) {
        return BCrypt.checkpw(password, hash);
    }
}