package com.studyflow.app.gui.admin;

import com.studyflow.app.exception.ArgumentNotValidException;
import com.studyflow.app.service.auth.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AdminPromoteLibrarianController {

    @FXML private TextField emailField;
    @FXML private TextField facilityIdField; // Yeni ID alanı
    @FXML private Label statusLabel;

    @Autowired
    private AuthService authService;

    // FacilityRepository kaldırıldı çünkü listeleme yapmıyoruz.

    @FXML
    public void initialize() {
        statusLabel.managedProperty().bind(statusLabel.visibleProperty());
    }

    @FXML
    public void handlePromoteUser() {
        String email = emailField.getText();
        String facilityIdStr = facilityIdField.getText();

        // 1. Basit Validasyonlar
        if (email == null || email.trim().isEmpty()) {
            showError("Please enter an email address.");
            return;
        }

        if (facilityIdStr == null || facilityIdStr.trim().isEmpty()) {
            showError("Please enter a Facility ID.");
            return;
        }

        // 2. ID Format Kontrolü (Sayı mı?)
        Long facilityId;
        try {
            facilityId = Long.parseLong(facilityIdStr.trim());
        } catch (NumberFormatException e) {
            showError("Facility ID must be a valid number.");
            return;
        }

        // 3. Servis Çağrısı
        try {
            // (Email, FacilityID)
            authService.registerLibrarian(email, facilityId);

            showSuccess("User successfully promoted to Librarian for Facility ID: " + facilityId);

            // Formu temizle
            emailField.clear();
            facilityIdField.clear();

        } catch (ArgumentNotValidException e) {
            showError("Operation failed: " + e.getMessage());
        } catch (Exception e) {
            showError("System Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #27ae60;"); // Yeşil
        statusLabel.setVisible(true);
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #e74c3c;"); // Kırmızı
        statusLabel.setVisible(true);
    }
}