package com.studyflow.app.gui.user.reservation;

import com.studyflow.app.context.GlobalParamsContext;
import com.studyflow.app.gui.NavigationService;
import com.studyflow.app.gui.ViewFactory;
import com.studyflow.app.gui.user.UserHomeController;
import com.studyflow.app.model.facility.FacilityBlock;
import com.studyflow.app.model.dto.AvailabilityDTO;
import com.studyflow.app.repository.facility.FacilityBlockRepository;
import com.studyflow.app.service.reservation.ReservationService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class UserReservationStep2Controller {

    @FXML private Pane mapCanvas;
    @FXML private Label dateLabel;

    @Autowired private GlobalParamsContext globalParams;
    @Autowired private FacilityBlockRepository blockRepository;
    @Autowired private ReservationService reservationService;
    @Autowired private NavigationService navigationService;
    @Autowired private ViewFactory viewFactory;

    @Autowired private UserHomeController userHomeController;

    @FXML
    public void initialize() {
        // Header Bilgisi
        dateLabel.setText(globalParams.getSelectedDate() + " | " + globalParams.getSelectedStartTime() + " - " + globalParams.getSelectedEndTime());

        loadBlocksWithAvailability();
    }

    private void loadBlocksWithAvailability() {
        mapCanvas.getChildren().clear();
        Long facilityId = globalParams.getSelectedFacility().getId();

        // 1. Blokları Çek
        List<FacilityBlock> blocks = blockRepository.findAllByFacilityId(facilityId);

        // 2. Doluluk Bilgisini Çek (DTO Listesi)
        List<AvailabilityDTO> availabilityList = reservationService.getBlockAvailability(
                facilityId,
                globalParams.getSelectedDate(),
                globalParams.getSelectedStartTime(),
                globalParams.getSelectedEndTime()
        );

        // Kolay erişim için Map'e çevir
        Map<Long, AvailabilityDTO> availabilityMap = availabilityList.stream()
                .collect(Collectors.toMap(AvailabilityDTO::getId, dto -> dto));

        // 3. Çizim
        for (FacilityBlock block : blocks) {
            AvailabilityDTO status = availabilityMap.get(block.getId());
            createBlockNode(block, status);
        }
    }

    // Scale factor: bloklari 1/2 boyutunda goster
    private static final double BLOCK_SCALE = 0.5;

    private void createBlockNode(FacilityBlock block, AvailabilityDTO status) {
        StackPane node = new StackPane();
        double scaledX = block.getX() * BLOCK_SCALE;
        double scaledY = block.getY() * BLOCK_SCALE;
        double scaledWidth = block.getWidth() * BLOCK_SCALE;
        double scaledHeight = block.getHeight() * BLOCK_SCALE;
        
        node.setLayoutX(scaledX);
        node.setLayoutY(scaledY);
        node.setPrefSize(scaledWidth, scaledHeight);

        Rectangle rect = new Rectangle(scaledWidth, scaledHeight);
        rect.setArcWidth(12);
        rect.setArcHeight(12);

        // RENK MANTIĞI - Gradient ile modern görünüm
        String colorHex;
        String gradientStart;
        String gradientEnd;
        boolean clickable = false;
        String statusText = "Dolu";
        int freeDesks = 0;

        if (status != null) {
            freeDesks = status.getFreeSeats(); // Blok için bu aslında free desk sayısı
            
            if ("GOOD".equals(status.getAvailabilityStatus())) {
                gradientStart = "#34d399";
                gradientEnd = "#10b981";
                colorHex = "#10b981";
                clickable = true;
                statusText = "Müsait";
            } else if ("LIMITED".equals(status.getAvailabilityStatus())) {
                gradientStart = "#fbbf24";
                gradientEnd = "#f59e0b";
                colorHex = "#f59e0b";
                clickable = true;
                statusText = "Sınırlı";
            } else {
                gradientStart = "#94a3b8";
                gradientEnd = "#64748b";
                colorHex = "#64748b";
            }
        } else {
            gradientStart = "#94a3b8";
            gradientEnd = "#64748b";
            colorHex = "#64748b";
        }

        // Gradient fill
        javafx.scene.paint.LinearGradient gradient = new javafx.scene.paint.LinearGradient(
                0, 0, 0, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web(gradientStart)),
                new javafx.scene.paint.Stop(1, Color.web(gradientEnd))
        );
        rect.setFill(gradient);
        rect.setStroke(Color.web(colorHex).darker());
        rect.setStrokeWidth(2);

        // Modern gölge efekti
        DropShadow shadow = new DropShadow(8, Color.rgb(0, 0, 0, 0.25));
        shadow.setOffsetY(3);
        rect.setEffect(shadow);

        // İçerik - İkon + İsim + Durum
        VBox contentBox = new VBox(4);
        contentBox.setAlignment(Pos.CENTER);

        Label nameLabel = new Label(block.getName());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; " +
                "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.4), 2, 0, 0, 1);");

        Label statusLabel = new Label(statusText);
        statusLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.9); -fx-font-size: 9px; " +
                "-fx-background-color: rgba(0,0,0,0.2); -fx-background-radius: 6; -fx-padding: 2 6;");

        contentBox.getChildren().addAll(nameLabel, statusLabel);

        node.getChildren().addAll(rect, contentBox);

        if (clickable) {
            node.setCursor(Cursor.HAND);

            // Modern hover efekti
            node.setOnMouseEntered(e -> {
                node.setScaleX(1.03);
                node.setScaleY(1.03);
                DropShadow hoverShadow = new DropShadow(15, Color.web(colorHex, 0.5));
                hoverShadow.setOffsetY(5);
                rect.setEffect(hoverShadow);
                rect.setStrokeWidth(3);
            });

            node.setOnMouseExited(e -> {
                node.setScaleX(1.0);
                node.setScaleY(1.0);
                DropShadow normalShadow = new DropShadow(8, Color.rgb(0, 0, 0, 0.25));
                normalShadow.setOffsetY(3);
                rect.setEffect(normalShadow);
                rect.setStrokeWidth(2);
            });

            node.setOnMouseClicked(e -> handleBlockClick(block));
        } else {
            node.setOpacity(0.5);
            node.setCursor(Cursor.DEFAULT);
        }

        mapCanvas.getChildren().add(node);
    }

    private void handleBlockClick(FacilityBlock block) {
        globalParams.setSelectedFacilityBlock(block);
        userHomeController.setView("/fxml/user/reservation/user-reservation-step3.fxml");
    }

    @FXML
    public void handleBack() {
        userHomeController.setView("/fxml/user/reservation/user-reservation-step1.fxml");
    }
}