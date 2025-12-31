package com.studyflow.app.gui;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MainController {
    @FXML
    private BorderPane mainPane;

    @Autowired
    private ViewFactory viewFactory;

    @Autowired
    private NavigationService navigationService;

    @FXML
    public void initialize() {
        navigationService.setMainLayout(mainPane);
        showLoginPage();
    }

    private void showLoginPage() {
        Parent loginView = viewFactory.loadView("/fxml/auth/login.fxml");
        navigationService.navigateTo(loginView);
    }

}
