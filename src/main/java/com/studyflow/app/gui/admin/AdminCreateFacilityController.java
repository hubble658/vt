package com.studyflow.app.gui.admin;

import com.studyflow.app.service.facility.FacilityService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AdminCreateFacilityController {

    @FXML private TextField nameField;
    @FXML private TextField addressField;
    @FXML private TextField imageField;
    @FXML private Label statusLabel;

    @Autowired
    private FacilityService facilityService;

    @Autowired
    private AdminMainController adminMainController;

    @FXML
    public void initialize() {
        statusLabel.managedProperty().bind(statusLabel.visibleProperty());
    }

    @FXML
    public void handleCreate() {
        String name = nameField.getText().trim();
        String address = addressField.getText().trim();
        String imageUrl = imageField.getText().trim();
        
        // Validation
        if (name.isEmpty()) {
            showError("Facility name is required");
            return;
        }
        
        if (address.isEmpty()) {
            showError("Address is required");
            return;
        }
        
        try {
            facilityService.createFacility(name, address, imageUrl.isEmpty() ? null : imageUrl);
            showSuccess("Facility created successfully!");
            
            // Clear fields
            nameField.clear();
            addressField.clear();
            imageField.clear();
            
            // Navigate back to list after 1.5 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    javafx.application.Platform.runLater(this::handleBack);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
        } catch (Exception e) {
            showError("Error: " + e.getMessage());
        }
    }

    @FXML
    public void handleBack() {
        adminMainController.loadViewFromExternal("/fxml/admin/admin-facilities-list.fxml");
    }
    
    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: 600;");
        statusLabel.setVisible(true);
    }
    
    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: 600;");
        statusLabel.setVisible(true);
    }
}
