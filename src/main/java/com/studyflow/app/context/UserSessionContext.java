package com.studyflow.app.context;

import com.studyflow.app.exception.NotAuthorizedException;
import com.studyflow.app.model.user.User;
import com.studyflow.app.model.user.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserSessionContext {
    private User activeUser;
    private Long assignedFacilityId;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void login(User user) {
        this.activeUser = user;
        // Librarian ise atanmış facility ID'sini yükle
        if (user != null && user.getUserRole() == UserRole.LIBRARIAN) {
            try {
                this.assignedFacilityId = jdbcTemplate.queryForObject(
                    "SELECT facility_id FROM librarian_facilities WHERE user_id = ?",
                    Long.class, user.getId());
            } catch (Exception e) {
                this.assignedFacilityId = null;
            }
        } else {
            this.assignedFacilityId = null;
        }
    }

    public User getCurrentUser() {
        return activeUser;
    }

    public void logout() {
        this.activeUser = null;
        this.assignedFacilityId = null;
    }
    
    public Long getAssignedFacilityId() {
        return assignedFacilityId;
    }

    public boolean isAuthenticated() {
        return activeUser != null;
    }

    public boolean isSameRole(UserRole neededRole){
        return activeUser.getUserRole().equals(neededRole);
    }

    public void checkAdmin() throws NotAuthorizedException {
        if (!activeUser.getUserRole().equals(UserRole.ADMIN)){
            throw new NotAuthorizedException("ADMIN");
        }
    }

    public void checkLibrarian() throws NotAuthorizedException {
        if (!activeUser.getUserRole().equals(UserRole.LIBRARIAN)){
            throw new NotAuthorizedException("LIBRARIAN");
        }
    }

}
