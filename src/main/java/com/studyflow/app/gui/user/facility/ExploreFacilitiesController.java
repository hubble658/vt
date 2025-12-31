package com.studyflow.app.gui.user.facility;

import com.studyflow.app.context.GlobalParamsContext;
import com.studyflow.app.gui.user.UserHomeController;
import com.studyflow.app.model.facility.Facility;
import com.studyflow.app.service.facility.FacilityService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class ExploreFacilitiesController {

    @FXML
    private ComboBox<String> sortComboBox;
    @FXML
    private FlowPane facilityGrid;

    @Autowired
    private FacilityService facilityService;
    @Autowired
    private UserHomeController userHomeController; // Layout değişimi için
    @Autowired
    private GlobalParamsContext globalParams; // Veri taşıma için

    @FXML
    public void initialize() {
        sortComboBox.getItems().addAll("Name (A-Z)", "Name (Z-A)");
        sortComboBox.setValue("Name (A-Z)");

        sortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String orderCode = newVal.equals("Name (Z-A)") ? "desc" : "asc";
                loadFacilities(orderCode);
            }
        });

        loadFacilities("asc");
    }

    @FXML
    public void handleBack() {
        userHomeController.showDashboard();
    }

    private void loadFacilities(String order) {
        facilityGrid.getChildren().clear();
        List<Facility> facilities = facilityService.getAllFacilities(order);

        if (facilities == null || facilities.isEmpty()) {
            Label emptyLabel = new Label("Tesis bulunamadi.");
            emptyLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #999;");
            facilityGrid.getChildren().add(emptyLabel);
            return;
        }

        for (Facility facility : facilities) {
            facilityGrid.getChildren().add(createFacilityCard(facility));
        }
    }

    private VBox createFacilityCard(Facility facility) {
        VBox card = new VBox();
        card.setPrefSize(300, 280);
        card.getStyleClass().add("facility-card");
        card.setAlignment(Pos.TOP_LEFT);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(298);
        imageView.setFitHeight(160);
        Rectangle clip = new Rectangle(298, 160);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        imageView.setClip(clip);

        try {
            String imageUrl = facility.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                if (imageUrl.startsWith("http")) {
                    // URL'den yukle
                    imageView.setImage(new Image(imageUrl, true));
                } else {
                    // Local resource'dan yukle
                    String imagePath = "/facility/" + imageUrl;
                    if (getClass().getResource(imagePath) != null) {
                        imageView
                                .setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath))));
                    }
                }
            }
        } catch (Exception e) {
        }

        VBox infoBox = new VBox(5);
        infoBox.setPadding(new Insets(15));

        Label nameLabel = new Label(facility.getName());
        nameLabel.getStyleClass().add("facility-title");
        nameLabel.setWrapText(true);

        Label addressLabel = new Label(facility.getAddress());
        addressLabel.getStyleClass().add("facility-address");
        addressLabel.setWrapText(true);

        Button viewButton = new Button("Goruntule & Rezerve Et");
        viewButton.getStyleClass().add("primary-button");
        viewButton.setMaxWidth(Double.MAX_VALUE);

        viewButton.setOnAction(e -> openFacilityDetails(facility));

        infoBox.getChildren().addAll(nameLabel, addressLabel, new Region(), viewButton);
        VBox.setVgrow(addressLabel, Priority.ALWAYS);

        card.getChildren().addAll(imageView, infoBox);
        card.setOnMouseClicked(e -> openFacilityDetails(facility));
        return card;
    }

    private void openFacilityDetails(Facility facility) {
        // 1. Veriyi Context'e at
        globalParams.setSelectedFacility(facility);

        // 2. Ana çerçeve içindeki görünümü değiştir (Header/Timer sabit kalır)
        userHomeController.showFacilityDashboard();
    }
}