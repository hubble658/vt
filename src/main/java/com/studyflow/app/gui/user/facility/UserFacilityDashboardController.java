package com.studyflow.app.gui.user.facility;

import com.studyflow.app.context.GlobalParamsContext;
import com.studyflow.app.gui.user.UserHomeController;
import com.studyflow.app.model.facility.Facility;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserFacilityDashboardController {

    @FXML
    private Label facilityNameLabel;
    @FXML
    private Button btnReserve;
    @FXML
    private Button btnHours;
    @FXML
    private Button btnInfo;

    @Autowired
    private UserHomeController userHomeController; // Layout Yöneticisi
    @Autowired
    private GlobalParamsContext globalParams; // Veri

    @FXML
    public void initialize() {
        setupButtonIcon(btnReserve, "📅", "icon-blue");
        setupButtonIcon(btnHours, "🕒", "icon-green");
        setupButtonIcon(btnInfo, "ℹ️", "icon-purple");

        // GÜNCELLEME: Rezervasyon butonu artık aktif
        btnReserve.setDisable(false);
        btnReserve.setText("Rezervasyon Yap");

        Facility facility = globalParams.getSelectedFacility();
        if (facility != null) {
            facilityNameLabel.setText(facility.getName());
        }
    }

    private void setupButtonIcon(Button btn, String iconText, String cssClass) {
        Label icon = new Label(iconText);
        icon.getStyleClass().add(cssClass);
        btn.setGraphic(icon);
    }

    @FXML
    public void handleReserve() {
        // Rezervasyon akışını başlat (Adım 1: Zaman Seçimi)
        userHomeController.setView("/fxml/user/reservation/user-reservation-step1.fxml");
    }

    @FXML
    public void handleWeeklyHours() {
        userHomeController.setView("/fxml/user/facility/user-facility-schedule.fxml");
    }

    @FXML
    public void handleInfo() {
        userHomeController.setView("/fxml/user/facility/user-facility-info.fxml");
    }

    @FXML
    public void handleBack() {
        userHomeController.showExploreFacilities();
    }
}