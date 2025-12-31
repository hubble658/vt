package com.studyflow.app.util.annotation;

import com.studyflow.app.exception.NotAuthorizedException;
import com.studyflow.app.context.UserSessionContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class SecurityAspect {
    @Autowired
    private UserSessionContext userSessionContext;

    @Before("@annotation(com.studyflow.app.util.annotation.RequireAdmin)")
    public void checkAdminPermission() throws NotAuthorizedException {
        userSessionContext.checkAdmin();
    }

    @Before("@annotation(com.studyflow.app.util.annotation.RequireLibrarian)")
    public void checkLibrarianPermission() throws NotAuthorizedException {
        userSessionContext.checkLibrarian();
    }
}
