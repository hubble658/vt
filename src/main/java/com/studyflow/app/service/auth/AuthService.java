package com.studyflow.app.service.auth;

import com.studyflow.app.context.UserSessionContext;
import com.studyflow.app.exception.ArgumentNotValidException;
import com.studyflow.app.exception.ResourceAlreadyExistException;
import com.studyflow.app.model.facility.Facility;
import com.studyflow.app.model.user.User;
import com.studyflow.app.model.user.UserRole;
import com.studyflow.app.repository.facility.FacilityRepository;
import com.studyflow.app.repository.user.UserRepository;
import com.studyflow.app.util.PasswordUtil;
import com.studyflow.app.util.ValidationUtil;
import com.studyflow.app.util.annotation.RequireAdmin;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthService {
    @Autowired
    private PasswordUtil passwordUtil;

    @Autowired
    private ValidationUtil validationUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionTimerService sessionTimerService;

    @Autowired
    private UserSessionContext userSessionContext;

    @Autowired
    private FacilityRepository facilityRepository;

    public void register(String email, String password, String firstName, String lastName) throws ResourceAlreadyExistException{
        User user = User.builder()
                .userRole(UserRole.USER)
                .email(email)
                .password(password)
                .firstName(firstName)
                .lastName(lastName).build();

        validationUtil.validate(user);
        validationUtil.validatePassword(password);

        if (userRepository.checkIfEmailExists(email) != 0){
            throw new ResourceAlreadyExistException("email");
        }

        userRepository.saveNewUser(email, passwordUtil.hashPassword(password), firstName, lastName, UserRole.USER.toString());
    }

    @Transactional
    public UserRole login(String email, String password) throws ArgumentNotValidException {
        validationUtil.validateEmail(email);
        User user = userRepository.getUserByEmailAddress(email);
        if (user == null){
            throw new ArgumentNotValidException(email);
        }
        String hashedPassword = user.getPassword();
        if (!passwordUtil.checkPassword(password, hashedPassword)){
            throw new ArgumentNotValidException("password");
        }
        userSessionContext.login(user);

        if (user.getUserRole().equals(UserRole.USER)){
            sessionTimerService.startTimer(10);
        } else if (user.getUserRole().equals(UserRole.ADMIN)){
            sessionTimerService.startTimer(30);
        } else {
            sessionTimerService.startTimer(60);
        }

        return user.getUserRole();
    }

    @RequireAdmin
    public void registerLibrarian(String email, Long facilityId){
        validationUtil.validateEmail(email);
        User user = userRepository.getUserByEmailAddress(email);
        if (user == null){
            throw new ArgumentNotValidException(email);
        }
        Facility facility = facilityRepository.getFacilityById(facilityId);
        if (facility == null){
            throw new ArgumentNotValidException(facilityId.toString());
        }
        userRepository.updateUserRole(email, UserRole.LIBRARIAN.toString());
        userRepository.insertUserFacility(user.getId(), facilityId);
    }


}
