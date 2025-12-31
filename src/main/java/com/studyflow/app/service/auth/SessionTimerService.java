package com.studyflow.app.service.auth;

import com.studyflow.app.context.UserSessionContext;
import com.studyflow.app.gui.NavigationService;
import com.studyflow.app.gui.ViewFactory;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SessionTimerService {
    private Timeline timeline;
    private int secondsRemaining;
    private int initialSeconds;

    private final StringProperty timeDisplay = new SimpleStringProperty("30:00");

    private final StringProperty timerStyle = new SimpleStringProperty("-fx-text-fill: #27ae60;");

    @Autowired
    private NavigationService navigationService;

    @Autowired
    private ViewFactory viewFactory;

    @Autowired
    private UserSessionContext userSessionContext;

    protected void startTimer(int minutes) {
        stopTimer();

        this.secondsRemaining = minutes * 60;
        this.initialSeconds = secondsRemaining;
        updateDisplay();

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            secondsRemaining--;
            updateDisplay();

            if (secondsRemaining <= 0) {
                handleTimeUp();
            }
        }));

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    protected void stopTimer() {
        if (timeline != null) {
            timeline.stop();
        }
    }

    public StringProperty timeDisplayProperty() {
        return timeDisplay;
    }

    public StringProperty timerStyleProperty() {
        return timerStyle;
    }

    private void updateDisplay() {
        long minutes = secondsRemaining / 60;
        long seconds = secondsRemaining % 60;
        String format = String.format("%02d:%02d", minutes, seconds);

        String colorCode;
        if (secondsRemaining > initialSeconds / 2) {
            colorCode = "#27ae60";
        } else if (secondsRemaining > initialSeconds / 5) {
            colorCode = "#f39c12";
        } else {
            colorCode = "#e74c3c";
        }

        final String style = "-fx-text-fill: " + colorCode + ";";

        Platform.runLater(() -> {
            timeDisplay.set(format);
            timerStyle.set(style);
        });
    }

    private void handleTimeUp() {
        stopTimer();
        userSessionContext.logout();

        Platform.runLater(() -> {
            navigationService.jumpTo(viewFactory.loadView("/fxml/auth/login.fxml"));
        });
    }
}