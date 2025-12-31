package com.studyflow.app.gui.user.facility;

import com.studyflow.app.context.GlobalParamsContext;
import com.studyflow.app.gui.user.UserHomeController;
import com.studyflow.app.model.facility.DailySchedule;
import com.studyflow.app.model.facility.Facility;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class UserFacilityDetailsController {

    @FXML private ImageView facilityImage;
    @FXML private Label nameLabel;
    @FXML private Label addressLabel;
    @FXML private VBox scheduleContainer;
    @FXML private Label headerLabel;

    @Autowired private UserHomeController userHomeController;
    @Autowired private GlobalParamsContext globalParams;

    private Facility currentFacility;

    @FXML
    public void initialize() {
        this.currentFacility = globalParams.getSelectedFacility();

        if (currentFacility != null) {
            if (headerLabel != null) headerLabel.setText(currentFacility.getName());
            if (nameLabel != null) populateInfo();
            if (scheduleContainer != null) populateSchedule();
        }
    }

    private void populateInfo() {
        nameLabel.setText(currentFacility.getName());
        addressLabel.setText(currentFacility.getAddress());
        try {
            String path = "/facility/" + currentFacility.getImageUrl();
            if (getClass().getResource(path) != null) {
                facilityImage.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(path))));
            }
        } catch (Exception e) {}
    }

    private void populateSchedule() {
        scheduleContainer.getChildren().clear();
        if (currentFacility.getWeeklyCalendar() == null) {
            scheduleContainer.getChildren().add(new Label("No schedule available."));
            return;
        }

        List<DailySchedule> days = currentFacility.getWeeklyCalendar().getDays();
        days.sort(Comparator.comparing(DailySchedule::getDayOfWeek));

        for (DailySchedule day : days) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setSpacing(20);
            row.setStyle("-fx-border-color: #eee; -fx-border-width: 0 0 1 0; -fx-padding: 15 0;");

            String dayName = day.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            Label dayLabel = new Label(dayName);
            dayLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #555; -fx-font-size: 16px;");
            dayLabel.setPrefWidth(140);

            Label timeLabel = new Label();
            timeLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 16px;");

            if (day.getIsClosed()) {
                timeLabel.setText("CLOSED");
                timeLabel.setStyle(timeLabel.getStyle() + "-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            } else {
                timeLabel.setText(day.getOpenTime() + " - " + day.getCloseTime());
                timeLabel.setStyle(timeLabel.getStyle() + "-fx-text-fill: #27ae60;");
            }

            row.getChildren().addAll(dayLabel, timeLabel);
            scheduleContainer.getChildren().add(row);
        }
    }

    @FXML
    public void handleBackToDashboard() {
        // Facility Dashboard'a geri dön (Veri zaten globalParams'da, tekrar set etmeye gerek yok)
        userHomeController.showFacilityDashboard();
    }
}