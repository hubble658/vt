package com.studyflow.app.gui.user;

import com.studyflow.app.gui.ViewFactory;
import com.studyflow.app.service.auth.SessionTimerService;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserHomeController {

    @FXML private Label timerLabel;
    @FXML private StackPane contentArea;

    @Autowired private SessionTimerService sessionTimerService;
    @Autowired private ViewFactory viewFactory;

    @FXML
    public void initialize() {
        if (timerLabel != null) {
            timerLabel.textProperty().bind(sessionTimerService.timeDisplayProperty());
            timerLabel.styleProperty().bind(sessionTimerService.timerStyleProperty());
        }
        showDashboard();
    }

    // --- PUBLIC NAVİGASYON METOTLARI ---

    public void showDashboard() {
        loadView("/fxml/user/user-dashboard.fxml");
    }

    public void showExploreFacilities() {
        loadView("/fxml/user/facility/explore-facilities.fxml");
    }

    public void showFacilityDashboard() {
        loadView("/fxml/user/facility/user-facility-dashboard.fxml");
    }

    // Diğer controllerların özel FXML yüklemesi için genel metot
    public void setView(String fxmlPath) {
        loadView(fxmlPath);
    }

    private void loadView(String fxmlPath) {
        try {
            Parent view = viewFactory.loadView(fxmlPath);
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}