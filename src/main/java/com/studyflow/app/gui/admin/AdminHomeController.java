package com.studyflow.app.gui.admin;

import com.studyflow.app.context.UserSessionContext;
import com.studyflow.app.gui.NavigationService;
import com.studyflow.app.gui.ViewFactory;
import com.studyflow.app.model.facility.Facility;
import com.studyflow.app.service.facility.FacilityService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class AdminHomeController {

    // Istatistik Labellari
    @FXML
    private Label totalFacilitiesLabel;
    @FXML
    private Label totalUsersLabel;
    @FXML
    private Label totalSeatsLabel;
    @FXML
    private Label activeReservationsLabel;
    @FXML
    private Label occupancyRateLabel;
    @FXML
    private Label totalBlocksLabel;
    @FXML
    private Label totalDesksLabel;
    @FXML
    private Label totalLibrariansLabel;

    // Pie Chart
    @FXML
    private PieChart reservationPieChart;

    // Top Facilities
    @FXML
    private FlowPane topFacilitiesPane;

    @Autowired
    private FacilityService facilityService;

    @Autowired
    private NavigationService navigationService;

    @Autowired
    private ViewFactory viewFactory;

    @Autowired
    private UserSessionContext userSessionContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @FXML
    public void initialize() {
        loadStatistics();
        loadTopFacilities();
    }

    private void loadStatistics() {
        try {
            // Temel istatistikler
            Integer facilities = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM facilities", Integer.class);
            Integer users = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE user_role = 'USER'",
                    Integer.class);
            Integer seats = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM seats", Integer.class);
            Integer blocks = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM facility_blocks", Integer.class);
            Integer desks = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM desks", Integer.class);
            Integer librarians = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE user_role = 'LIBRARIAN'",
                    Integer.class);

            // Rezervasyon istatistikleri
            Integer activeRes = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM reservations WHERE status = 'ACTIVE' AND reservation_date >= ?",
                    Integer.class, LocalDate.now());
            Integer completedRes = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM reservations WHERE status = 'COMPLETED'", Integer.class);
            Integer cancelledRes = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM reservations WHERE status = 'CANCELLED'", Integer.class);

            // Bugunku doluluk orani
            Integer todayReservations = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM reservations WHERE reservation_date = ? AND status = 'ACTIVE'",
                    Integer.class, LocalDate.now());
            double occupancyRate = seats > 0 ? (todayReservations * 100.0 / seats) : 0;

            // Labellari guncelle
            totalFacilitiesLabel.setText(String.valueOf(facilities != null ? facilities : 0));
            totalUsersLabel.setText(String.valueOf(users != null ? users : 0));
            totalSeatsLabel.setText(String.valueOf(seats != null ? seats : 0));
            activeReservationsLabel.setText(String.valueOf(activeRes != null ? activeRes : 0));
            totalBlocksLabel.setText(String.valueOf(blocks != null ? blocks : 0));
            totalDesksLabel.setText(String.valueOf(desks != null ? desks : 0));
            totalLibrariansLabel.setText(String.valueOf(librarians != null ? librarians : 0));
            occupancyRateLabel.setText(String.format("%.1f%%", occupancyRate));

            // Pie Chart verilerini guncelle
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                    new PieChart.Data("Active (" + (activeRes != null ? activeRes : 0) + ")",
                            activeRes != null ? activeRes : 0),
                    new PieChart.Data("Completed (" + (completedRes != null ? completedRes : 0) + ")",
                            completedRes != null ? completedRes : 0),
                    new PieChart.Data("Cancelled (" + (cancelledRes != null ? cancelledRes : 0) + ")",
                            cancelledRes != null ? cancelledRes : 0));
            reservationPieChart.setData(pieData);

            // Pie Chart renkleri
            String[] colors = { "#27ae60", "#3498db", "#e74c3c" };
            for (int i = 0; i < pieData.size(); i++) {
                pieData.get(i).getNode().setStyle("-fx-pie-color: " + colors[i] + ";");
            }

        } catch (Exception e) {
            System.err.println("Istatistik yukleme hatasi: " + e.getMessage());
        }
    }

    private void loadTopFacilities() {
        try {
            List<Facility> facilities = facilityService.getAllFacilities("ASC");
            topFacilitiesPane.getChildren().clear();

            // En fazla 4 tesis goster
            int limit = Math.min(4, facilities.size());
            for (int i = 0; i < limit; i++) {
                Facility f = facilities.get(i);
                topFacilitiesPane.getChildren().add(createFacilityMiniCard(f));
            }

            if (facilities.isEmpty()) {
                Label empty = new Label("Henuz tesis yok");
                empty.setStyle("-fx-text-fill: #999; -fx-font-size: 14px;");
                topFacilitiesPane.getChildren().add(empty);
            }
        } catch (Exception e) {
            System.err.println("Top facilities yukleme hatasi: " + e.getMessage());
        }
    }

    private VBox createFacilityMiniCard(Facility facility) {
        VBox card = new VBox(8);
        card.setPrefWidth(180);
        card.setPrefHeight(140);
        card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 10;");
        card.setAlignment(Pos.TOP_CENTER);

        // Image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(160);
        imageView.setFitHeight(80);
        imageView.setPreserveRatio(false);
        imageView.setStyle("-fx-background-radius: 5;");

        try {
            if (facility.getImageUrl() != null && !facility.getImageUrl().isEmpty()) {
                imageView.setImage(new Image(facility.getImageUrl(), true));
            }
        } catch (Exception e) {
            // Default image
        }

        // Name
        Label name = new Label(facility.getName());
        name.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #333;");
        name.setWrapText(true);

        // Seats count
        Integer seatCount = getSeatCount(facility.getId());
        Label seats = new Label(seatCount + " koltuk");
        seats.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        card.getChildren().addAll(imageView, name, seats);
        return card;
    }

    private int getSeatCount(Long facilityId) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(s.id) FROM seats s " +
                            "JOIN desks d ON s.desk_id = d.id " +
                            "JOIN facility_blocks fb ON d.block_id = fb.id " +
                            "WHERE fb.facility_id = ?",
                    Integer.class, facilityId);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @FXML
    public void handleGoToCreateFacility() {
        navigateToView("/fxml/admin/admin-create-facility.fxml");
    }

    @FXML
    public void handleViewAllFacilities() {
        navigateToView("/fxml/admin/admin-facilities-list.fxml");
    }

    private void navigateToView(String fxmlPath) {
        try {
            // Parent stackpane'i bul
            Parent parent = topFacilitiesPane.getParent();
            while (parent != null && !(parent.getParent() instanceof StackPane)) {
                parent = parent.getParent();
            }
            if (parent != null && parent.getParent() instanceof StackPane) {
                StackPane contentArea = (StackPane) parent.getParent();
                Parent view = viewFactory.loadView(fxmlPath);
                contentArea.getChildren().setAll(view);
            }
        } catch (Exception e) {
            System.err.println("Navigation hatasi: " + e.getMessage());
        }
    }
}