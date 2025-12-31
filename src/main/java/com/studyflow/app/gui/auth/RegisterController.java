package com.studyflow.app.gui.auth;

import com.studyflow.app.exception.ArgumentNotValidException;
import com.studyflow.app.exception.ResourceAlreadyExistException;
import com.studyflow.app.gui.NavigationService;
import com.studyflow.app.service.auth.AuthService;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RegisterController {
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;

    // Registerde hatali girdi animasyonlari
    @FXML private TranslateTransition shakePassAnim;
    @FXML private TranslateTransition shakeConfirmAnim;
    @FXML private TranslateTransition shakeFirstNameAnim;
    @FXML private TranslateTransition shakeLastNameAnim;
    @FXML private TranslateTransition shakeEmailAnim;

    @Autowired private NavigationService navigationService;

    @Autowired private AuthService authService;

    @FXML
    public void initialize() {
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
    }

    @FXML
    public void handleRegister() {
        resetFieldStyles();

        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPass = confirmPasswordField.getText();

        boolean hasError = false;

        // 1 - Empty field check
        if (firstName.isEmpty()) {
            addErrorStyle(firstNameField);
            shakeFirstNameAnim.playFromStart();
            hasError = true;
        }
        if (lastName.isEmpty()) {
            addErrorStyle(lastNameField);
            shakeLastNameAnim.playFromStart();
            hasError = true;
        }
        if (email.isEmpty()) {
            addErrorStyle(emailField);
            shakeEmailAnim.playFromStart();
            hasError = true;
        }
        if (password.isEmpty()) {
            addErrorStyle(passwordField);
            shakePassAnim.playFromStart();
            hasError = true;
        }
        if (confirmPass.isEmpty()) {
            addErrorStyle(confirmPasswordField);
            shakeConfirmAnim.playFromStart();
            hasError = true;
        }

        if (hasError) {
            showError("Please fill in all required fields.");
            return;
        }

        // 2 - Check password equality
        if (!password.equals(confirmPass)) {
            showError("Passwords do not match!");

            addErrorStyle(passwordField);
            addErrorStyle(confirmPasswordField);

            shakePassAnim.playFromStart();
            shakeConfirmAnim.playFromStart();
            return;
        }

        // 3 - Send to backend
        try {
            authService.register(email, password, firstName, lastName);
            System.out.println("Register success: " + email);
            navigationService.goBack();
        } catch (ArgumentNotValidException e){
            if (e.getMessage().equals("password")){
                showError("Please enter a valid password! Length must be between 6-20");
                addErrorStyle(passwordField);
                addErrorStyle(confirmPasswordField);
                shakePassAnim.playFromStart();
                shakeConfirmAnim.playFromStart();
            } else if (e.getMessage().contains("email")){
                showError("Please enter a valid email address!");
                addErrorStyle(emailField);
                shakeEmailAnim.playFromStart();
            } else {
                showError("First name and last name must be in proper length! Contact support if your name is correct.");
                addErrorStyle(firstNameField);
                shakeFirstNameAnim.playFromStart();
                addErrorStyle(lastNameField);
                shakeLastNameAnim.playFromStart();
            }
        } catch (ResourceAlreadyExistException e){
            showError("The email you entered is in use. Please login or use a different email address.");
            addErrorStyle(emailField);
            shakeEmailAnim.playFromStart();
        }
    }

    private void resetFieldStyles() {
        removeErrorStyle(firstNameField);
        removeErrorStyle(lastNameField);
        removeErrorStyle(emailField);
        removeErrorStyle(passwordField);
        removeErrorStyle(confirmPasswordField);
        errorLabel.setVisible(false);
    }

    private void removeErrorStyle(Node node) {
        node.getStyleClass().remove("has-error");
    }

    private void addErrorStyle(Node node) {
        if (!node.getStyleClass().contains("has-error")) {
            node.getStyleClass().add("has-error");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    @FXML
    public void handleGoToLogin() {
        navigationService.goBack();
    }
}