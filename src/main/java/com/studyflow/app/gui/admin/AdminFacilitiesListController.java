package com.studyflow.app.gui.admin;

import com.studyflow.app.gui.ViewFactory;
import com.studyflow.app.model.facility.Facility;
import com.studyflow.app.repository.facility.FacilityRepository;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdminFacilitiesListController {

    @FXML private TextField searchField;
    @FXML private FlowPane facilitiesContainer;
    @FXML private VBox emptyState;
    @FXML private Label totalCountLabel;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ViewFactory viewFactory;

    @Autowired
    private AdminMainController adminMainController;

    @FXML
    public void initialize() {
        loadFacilities();
    }

    private void loadFacilities() {
        List<Facility> facilities = facilityRepository.findAll();
        displayFacilities(facilities);
    }

    @FXML
    public void handleSearch() {
        String query = searchField.getText().trim().toLowerCase();
        List<Facility> facilities;
        
        if (query.isEmpty()) {
            facilities = facilityRepository.findAll();
        } else {
            facilities = facilityRepository.findAll().stream()
                .filter(f -> f.getName().toLowerCase().contains(query) || 
                            f.getAddress().toLowerCase().contains(query))
                .toList();
        }
        
        displayFacilities(facilities);
    }

    private void displayFacilities(List<Facility> facilities) {
        facilitiesContainer.getChildren().clear();
        
        if (facilities.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            totalCountLabel.setText("Total: 0 facilities");
            return;
        }
        
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        totalCountLabel.setText("Total: " + facilities.size() + " facilities");
        
        for (Facility facility : facilities) {
            VBox card = createFacilityCard(facility);
            facilitiesContainer.getChildren().add(card);
        }
    }

    private VBox createFacilityCard(Facility facility) {
        VBox card = new VBox(12);
        card.setPrefWidth(320);
        card.setPrefHeight(280);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 0;");
        card.setEffect(new DropShadow(15, Color.rgb(0, 0, 0, 0.08)));
        
        // Facility Image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(320);
        imageView.setFitHeight(140);
        imageView.setPreserveRatio(false);
        imageView.setStyle("-fx-background-radius: 16 16 0 0;");
        
        try {
            if (facility.getImageUrl() != null && !facility.getImageUrl().isEmpty()) {
                imageView.setImage(new Image(facility.getImageUrl(), true));
            }
        } catch (Exception e) {
            // Default placeholder
        }
        
        // Content
        VBox content = new VBox(8);
        content.setPadding(new Insets(15));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 16 16;");
        
        // Name
        Label nameLabel = new Label(facility.getName());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #1e293b;");
        nameLabel.setWrapText(true);
        
        // Address
        Label addressLabel = new Label(facility.getAddress());
        addressLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        addressLabel.setWrapText(true);
        
        // Stats row
        HBox statsRow = new HBox(15);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        
        // Get stats for this facility
        Integer blockCount = getBlockCount(facility.getId());
        Integer seatCount = getSeatCount(facility.getId());
        Integer activeRes = getActiveReservations(facility.getId());
        
        Label blocksLabel = new Label("Blocks: " + blockCount);
        blocksLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #3b82f6; -fx-font-weight: 600;");
        
        Label seatsLabel = new Label("Seats: " + seatCount);
        seatsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981; -fx-font-weight: 600;");
        
        Label resLabel = new Label("Active: " + activeRes);
        resLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #f59e0b; -fx-font-weight: 600;");
        
        statsRow.getChildren().addAll(blocksLabel, seatsLabel, resLabel);
        
        // Actions
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        Button viewBtn = new Button("View Details");
        viewBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 6; -fx-cursor: hand;");
        viewBtn.setOnAction(e -> viewFacilityDetails(facility));
        
        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 6; -fx-cursor: hand;");
        editBtn.setOnAction(e -> editFacility(facility));
        
        actions.getChildren().addAll(editBtn, viewBtn);
        
        content.getChildren().addAll(nameLabel, addressLabel, statsRow, actions);
        VBox.setVgrow(content, Priority.ALWAYS);
        
        card.getChildren().addAll(imageView, content);
        
        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 0; -fx-scale-x: 1.02; -fx-scale-y: 1.02;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 0;"));
        
        return card;
    }
    
    private Integer getBlockCount(Long facilityId) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM facility_blocks WHERE facility_id = ?", 
                Integer.class, facilityId);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private Integer getSeatCount(Long facilityId) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM seats s " +
                "JOIN desks d ON s.desk_id = d.id " +
                "JOIN facility_blocks fb ON d.block_id = fb.id " +
                "WHERE fb.facility_id = ?", 
                Integer.class, facilityId);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private Integer getActiveReservations(Long facilityId) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservations WHERE facility_id = ? AND status = 'ACTIVE'", 
                Integer.class, facilityId);
        } catch (Exception e) {
            return 0;
        }
    }

    @FXML
    public void handleCreateNew() {
        adminMainController.loadViewFromExternal("/fxml/admin/admin-create-facility.fxml");
    }
    
    private void viewFacilityDetails(Facility facility) {
        // TODO: Navigate to facility details
        System.out.println("View details for: " + facility.getName());
    }
    
    private void editFacility(Facility facility) {
        // TODO: Navigate to edit facility
        System.out.println("Edit facility: " + facility.getName());
    }
}
