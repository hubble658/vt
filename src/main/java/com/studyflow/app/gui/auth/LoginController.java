package com.studyflow.app.gui.auth;

import com.studyflow.app.exception.ArgumentNotValidException;
import com.studyflow.app.gui.NavigationService;
import com.studyflow.app.gui.ViewFactory;
import com.studyflow.app.model.user.UserRole;
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
public class LoginController {

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    @FXML
    private TranslateTransition shakeEmailAnim;
    @FXML
    private TranslateTransition shakePassAnim;

    @Autowired
    private NavigationService navigationService;

    @Autowired
    private ViewFactory viewFactory;

    @Autowired
    private AuthService authService;

    @FXML
    public void initialize() {
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
    }

    @FXML
    public void handleLogin() {
        resetFieldStyles();

        String email = emailField.getText();
        String password = passwordField.getText();
        boolean hasError = false;

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

        if (hasError) {
            showError("Lutfen e-posta ve sifrenizi girin.");
            return;
        }

        try {
            UserRole userole = authService.login(email, password);
            if (userole.equals(UserRole.USER)) {
                navigationService.navigateTo(viewFactory.loadView("/fxml/user/user-home.fxml"));
            } else if (userole.equals(UserRole.ADMIN)) {
                navigationService.navigateTo(viewFactory.loadView("/fxml/admin/admin-main-layout.fxml"));
            } else {
                navigationService.navigateTo(viewFactory.loadView("/fxml/librarian/librarian-main-layout.fxml"));
            }
            System.out.println("Login Success: " + email);
        } catch (ArgumentNotValidException e) {
            showError("E-posta adresi veya sifre hatali!");
        }
    }

    @FXML
    public void handleGoToRegister() {
        navigationService.navigateTo(viewFactory.loadView("/fxml/auth/register.fxml"));
    }

    private void resetFieldStyles() {
        removeErrorStyle(emailField);
        removeErrorStyle(passwordField);
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
}