package com.studyflow.app.gui.user.reservation;

import com.studyflow.app.context.GlobalParamsContext;
import com.studyflow.app.gui.NavigationService;
import com.studyflow.app.gui.ViewFactory;
import com.studyflow.app.gui.user.UserHomeController;
import com.studyflow.app.model.facility.Desk;
import com.studyflow.app.model.facility.Facility;
import com.studyflow.app.model.facility.FacilityBlock;
import com.studyflow.app.model.dto.AvailabilityDTO;
import com.studyflow.app.repository.facility.DeskRepository;
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

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class UserReservationStep3Controller {

    private static final double MAP_LIMIT = 800.0;
    // Scale factor: masalari 1/2 boyutunda goster
    private static final double DESK_SCALE = 0.5;

    @FXML
    private Pane deskCanvas;

    // Header sağ üst özet (Step4 ile uyumlu)
    @FXML
    private Label lblTopSummaryDateTime;
    @FXML
    private Label lblTopSummaryBlockInfo;

    @Autowired
    private GlobalParamsContext globalParams;
    @Autowired
    private DeskRepository deskRepository;
    @Autowired
    private ReservationService reservationService;
    @Autowired
    private NavigationService navigationService;
    @Autowired
    private ViewFactory viewFactory;
    @Autowired
    private UserHomeController userHomeController;

    @FXML
    public void initialize() {
        FacilityBlock block = globalParams.getSelectedFacilityBlock();
        if (block == null) {
            // Güvenli fallback: blok yoksa bir önceki adıma dön
            userHomeController.setView("/fxml/user/reservation/user-reservation-step2.fxml");
            return;
        }

        // Canvas boyutunu her zaman sabit 600x600 yap (Desk Editor ile aynı)
        deskCanvas.setPrefSize(MAP_LIMIT, MAP_LIMIT);
        deskCanvas.setMinSize(MAP_LIMIT, MAP_LIMIT);
        deskCanvas.setMaxSize(MAP_LIMIT, MAP_LIMIT);

        setupHeaderSummary(block);
        loadDesksWithAvailability(block);
    }

    private void setupHeaderSummary(FacilityBlock block) {
        // Tarih + Saat
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String dateText = globalParams.getSelectedDate() != null
                ? globalParams.getSelectedDate().format(dateFormatter)
                : "-";

        String timeText = globalParams.getSelectedStartTime() + " – " + globalParams.getSelectedEndTime();

        if (lblTopSummaryDateTime != null) {
            lblTopSummaryDateTime.setText(dateText + "  |  " + timeText);
        }

        // Facility + Block bilgisi
        Facility facility = globalParams.getSelectedFacility();
        String facilityName = facility != null ? facility.getName() : "-";
        String blockName = block.getName() != null ? block.getName() : "-";

        if (lblTopSummaryBlockInfo != null) {
            lblTopSummaryBlockInfo.setText(facilityName + "  •  " + blockName);
        }
    }

    private void loadDesksWithAvailability(FacilityBlock block) {
        deskCanvas.getChildren().clear();
        List<Desk> desks = deskRepository.findAllByBlockId(block.getId());

        List<AvailabilityDTO> availabilityList = reservationService.getDeskAvailability(
                block.getId(),
                globalParams.getSelectedDate(),
                globalParams.getSelectedStartTime(),
                globalParams.getSelectedEndTime());

        Map<Long, AvailabilityDTO> availabilityMap = availabilityList.stream()
                .collect(Collectors.toMap(AvailabilityDTO::getId, dto -> dto));

        for (Desk desk : desks) {
            AvailabilityDTO status = availabilityMap.get(desk.getId());
            createDeskNode(desk, status);
        }
    }

    private void createDeskNode(Desk desk, AvailabilityDTO status) {
        StackPane node = new StackPane();
        double scaledX = desk.getX() * DESK_SCALE;
        double scaledY = desk.getY() * DESK_SCALE;
        double scaledWidth = desk.getWidth() * DESK_SCALE;
        double scaledHeight = desk.getHeight() * DESK_SCALE;

        node.setLayoutX(scaledX);
        node.setLayoutY(scaledY);
        node.setPrefSize(scaledWidth, scaledHeight);

        Rectangle rect = new Rectangle(scaledWidth, scaledHeight);
        rect.setArcWidth(12);
        rect.setArcHeight(12);

        // ---- RENK / DURUM HESABI ----
        String colorHex;
        String gradientStart;
        String gradientEnd;
        boolean clickable = false;
        int freeSeats = 0;
        Integer totalSeats = null;

        if (status == null) {
            // Hiç kayıt yok = tüm koltukları boş kabul ediyoruz
            gradientStart = "#34d399";
            gradientEnd = "#10b981";
            colorHex = "#10b981";
            clickable = true;
        } else {
            freeSeats = status.getFreeSeats();
            try {
                totalSeats = status.getTotalSeats();
            } catch (Exception ignored) {
                totalSeats = null;
            }

            if (freeSeats == 0) {
                // FULL
                gradientStart = "#94a3b8";
                gradientEnd = "#64748b";
                colorHex = "#64748b";
                clickable = false;
            } else {
                // LIMITED vs AVAILABLE
                boolean isLimited;
                if (totalSeats != null && totalSeats > 0) {
                    double ratio = (double) freeSeats / totalSeats;
                    isLimited = ratio <= 0.50;
                } else {
                    isLimited = freeSeats <= 2;
                }

                if (isLimited) {
                    gradientStart = "#fbbf24";
                    gradientEnd = "#f59e0b";
                    colorHex = "#f59e0b";
                } else {
                    gradientStart = "#34d399";
                    gradientEnd = "#10b981";
                    colorHex = "#10b981";
                }
                clickable = true;
            }
        }

        // Gradient fill
        javafx.scene.paint.LinearGradient gradient = new javafx.scene.paint.LinearGradient(
                0, 0, 0, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web(gradientStart)),
                new javafx.scene.paint.Stop(1, Color.web(gradientEnd)));
        rect.setFill(gradient);
        rect.setStroke(Color.web(colorHex).darker());
        rect.setStrokeWidth(2);

        // Modern gölge efekti
        DropShadow shadow = new DropShadow(8, Color.rgb(0, 0, 0, 0.25));
        shadow.setOffsetY(3);
        rect.setEffect(shadow);

        // ---- METİN: Üstte IdRange, altta subtitle ----
        String deskText = (desk.getIdRange() != null && !desk.getIdRange().isBlank())
                ? desk.getIdRange()
                : ("Masa " + desk.getId());

        String subtitle;
        if (status == null) {
            subtitle = "Tum koltuklar bos";
        } else if (freeSeats == 0) {
            subtitle = "Bos koltuk yok";
        } else if (freeSeats == 1) {
            subtitle = "1 bos koltuk";
        } else {
            subtitle = freeSeats + " bos koltuk";
        }

        Label titleLabel = new Label(deskText);
        titleLabel.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 10px;" +
                        "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.4), 2, 0, 0, 1);");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.9);" +
                        "-fx-font-size: 8px;" +
                        "-fx-background-color: rgba(0,0,0,0.2);" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 2 5;");

        VBox textBox = new VBox(4, titleLabel, subtitleLabel);
        textBox.setAlignment(Pos.CENTER);

        node.getChildren().addAll(rect, textBox);

        // ---- Sadece seçilebilir desk'lere hover + click ----
        if (clickable) {
            node.setCursor(Cursor.HAND);

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

            node.setOnMouseClicked(e -> handleDeskClick(desk));
        } else {
            node.setOpacity(0.5);
            node.setCursor(Cursor.DEFAULT);
        }

        deskCanvas.getChildren().add(node);
    }

    private void handleDeskClick(Desk desk) {
        globalParams.setSelectedDesk(desk);
        userHomeController.setView("/fxml/user/reservation/user-reservation-step4.fxml");
    }

    @FXML
    public void handleBack() {
        userHomeController.setView("/fxml/user/reservation/user-reservation-step2.fxml");
    }
}
