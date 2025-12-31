package com.studyflow.app.gui.librarian;

import com.studyflow.app.service.facility.FacilityService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class LibrarianScheduleController {

    @FXML
    private ComboBox<DayOfWeek> dayComboBox;
    @FXML
    private CheckBox closedCheckBox;
    @FXML
    private ComboBox<LocalTime> openTimeBox;
    @FXML
    private ComboBox<LocalTime> closeTimeBox;
    @FXML
    private HBox timeContainer;
    @FXML
    private Label statusLabel;

    @Autowired
    private FacilityService facilityService;

    private final ObservableList<LocalTime> allTimeSlots = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        statusLabel.managedProperty().bind(statusLabel.visibleProperty());

        // Günleri doldur
        dayComboBox.getItems().setAll(DayOfWeek.values());

        // Saat dilimlerini oluştur
        generateTimeSlots();
        openTimeBox.setItems(allTimeSlots);
        closeTimeBox.setItems(FXCollections.observableArrayList(allTimeSlots));

        openTimeBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateCloseTimeOptions(newVal);
            }
        });

        closedCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            timeContainer.setDisable(newVal);
            if (newVal) {
                openTimeBox.getSelectionModel().clearSelection();
                closeTimeBox.getSelectionModel().clearSelection();
            }
        });

        openTimeBox.setValue(LocalTime.of(9, 0));
        closeTimeBox.setValue(LocalTime.of(18, 0));
    }

    private void generateTimeSlots() {
        LocalTime time = LocalTime.of(0, 0);
        while (true) {
            allTimeSlots.add(time);
            time = time.plusMinutes(10);
            if (time.equals(LocalTime.of(0, 0))) {
                break;
            }
        }
    }

    private void updateCloseTimeOptions(LocalTime openTime) {
        List<LocalTime> validCloseTimes = new ArrayList<>();

        for (LocalTime time : allTimeSlots) {
            if (time.isAfter(openTime)) {
                validCloseTimes.add(time);
            }
        }

        closeTimeBox.setItems(FXCollections.observableArrayList(validCloseTimes));

        LocalTime currentClose = closeTimeBox.getValue();
        if (currentClose != null && !currentClose.isAfter(openTime)) {
            closeTimeBox.getSelectionModel().clearSelection();
        }
    }

    @FXML
    public void handleAddSchedule() {
        try {
            DayOfWeek selectedDay = dayComboBox.getValue();
            if (selectedDay == null) {
                showError("Please select a day.");
                return;
            }

            boolean isClosed = closedCheckBox.isSelected();
            LocalTime openTime = null;
            LocalTime closeTime = null;

            if (!isClosed) {
                openTime = openTimeBox.getValue();
                closeTime = closeTimeBox.getValue();

                if (openTime == null || closeTime == null) {
                    showError("Please select both open and close times.");
                    return;
                }
            }

            facilityService.addDailySchedule(selectedDay, openTime, closeTime, isClosed);
            showSuccess("Schedule updated for " + selectedDay);

        } catch (Exception e) {
            showError("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #27ae60;");
        statusLabel.setVisible(true);
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #e74c3c;");
        statusLabel.setVisible(true);
    }
}
