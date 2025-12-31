package com.studyflow.app.gui.user.reservation;

import com.studyflow.app.context.GlobalParamsContext;
import com.studyflow.app.gui.NavigationService;
import com.studyflow.app.gui.ViewFactory;
import com.studyflow.app.gui.user.UserHomeController;
import com.studyflow.app.model.facility.Desk;
import com.studyflow.app.model.facility.Facility;
import com.studyflow.app.model.facility.FacilityBlock;
import com.studyflow.app.model.facility.Seat;
import com.studyflow.app.repository.facility.SeatRepository;
import com.studyflow.app.service.reservation.ReservationService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class UserReservationStep4Controller {

    private static final double CANVAS_SIZE = 600.0;
    private static final double SEAT_SIZE = 32.0;

    @FXML
    private Pane seatCanvas;
    @FXML
    private StackPane confirmOverlay;
    @FXML
    private BorderPane mainLayout;

    // Header sağ üst özet
    @FXML
    private Label lblTopSummaryDateTime;
    @FXML
    private Label lblTopSummaryBlockDesk;

    // Confirm modal alanları
    @FXML
    private Label lblModalTitle;
    @FXML
    private Label lblConfirmDate;
    @FXML
    private Label lblConfirmTime;
    @FXML
    private Label lblConfirmFacility;
    @FXML
    private Label lblConfirmBlock;
    @FXML
    private Label lblConfirmDesk;
    @FXML
    private Label lblConfirmSeat;

    @FXML
    private Label lblWarningMessage; // 30 dakika uyarısı
    @FXML
    private Label lblErrorMessage; // Hata mesajı
    @FXML
    private Label lblSuccessMessage; // Başarılı rezervasyon mesajı

    @FXML
    private HBox confirmButtonsRow; // Cancel + Confirm
    @FXML
    private HBox errorButtonsRow; // Tek "OK" butonu
    @FXML
    private HBox successButtonsRow; // Başarı sonrası "Back to Home"

    @Autowired
    private GlobalParamsContext globalParams;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private ReservationService reservationService;
    @Autowired
    private NavigationService navigationService;
    @Autowired
    private ViewFactory viewFactory;
    @Autowired
    private UserHomeController userHomeController;

    private Desk currentDesk;
    private StackPane selectedSeatNode = null;

    // Masa origin'i (canvas içinde ortalanmış halinin sol üst köşesi)
    private double deskOriginX;
    private double deskOriginY;

    // Periyodik refresh için
    private Timeline refreshTimeline;

    @FXML
    public void initialize() {
        currentDesk = globalParams.getSelectedDesk();
        if (currentDesk == null) {
            userHomeController.setView("/fxml/user/reservation/user-reservation-step3.fxml");
            return;
        }

        seatCanvas.setPrefSize(CANVAS_SIZE, CANVAS_SIZE);
        seatCanvas.setMinSize(CANVAS_SIZE, CANVAS_SIZE);
        seatCanvas.setMaxSize(CANVAS_SIZE, CANVAS_SIZE);

        setupHeaderSummary();
        renderScene();
        startRefreshTimer();
    }

    private void setupHeaderSummary() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String dateText = globalParams.getSelectedDate() != null
                ? globalParams.getSelectedDate().format(dateFormatter)
                : "-";

        String timeText = globalParams.getSelectedStartTime() + " – " + globalParams.getSelectedEndTime();
        if (lblTopSummaryDateTime != null) {
            lblTopSummaryDateTime.setText(dateText + "  |  " + timeText);
        }

        Facility facility = globalParams.getSelectedFacility();
        FacilityBlock block = globalParams.getSelectedFacilityBlock();

        String facilityName = (facility != null) ? facility.getName() : "-";
        String blockName = (block != null) ? block.getName() : "-";
        String deskText = (currentDesk.getIdRange() != null)
                ? currentDesk.getIdRange()
                : ("Masa " + currentDesk.getId());

        if (lblTopSummaryBlockDesk != null) {
            lblTopSummaryBlockDesk.setText(
                    facilityName + "  •  " + blockName + "  •  " + deskText);
        }
    }

    // Her 10 saniyede bir seat doluluğunu yenile
    private void startRefreshTimer() {
        refreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(10), e -> {
                    // Kullanıcı confirm/success ekranındaysa ekranı zıplatmayalım
                    if (!confirmOverlay.isVisible()) {
                        renderScene();
                    }
                }));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private void stopRefreshTimer() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
            refreshTimeline = null;
        }
    }

    private void renderScene() {
        seatCanvas.getChildren().clear();
        selectedSeatNode = null; // refresh sonrası selection reset

        double deskW = currentDesk.getWidth();
        double deskH = currentDesk.getHeight();

        // Masayı canvas'ın ortasına yerleştir
        deskOriginX = (CANVAS_SIZE - deskW) / 2.0;
        deskOriginY = (CANVAS_SIZE - deskH) / 2.0;

        Rectangle deskRect = new Rectangle(deskW, deskH);
        deskRect.setX(deskOriginX);
        deskRect.setY(deskOriginY);

        String deskColor = currentDesk.getColorHex() != null
                ? currentDesk.getColorHex()
                : "#8e44ad";

        try {
            deskRect.setFill(Color.web(deskColor));
        } catch (Exception e) {
            deskRect.setFill(Color.PURPLE);
        }

        deskRect.setOpacity(0.5);
        deskRect.setStroke(Color.DARKGRAY);
        deskRect.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.40)));

        Label deskLabel = new Label(currentDesk.getIdRange() != null ? currentDesk.getIdRange() : "Masa");
        deskLabel.setStyle(
                "-fx-font-size: 20px;" +
                        "-fx-text-fill: rgba(0,0,0,0.35);" +
                        "-fx-font-weight: bold;");
        deskLabel.setLayoutX(deskOriginX + (deskW / 2) - 28);
        deskLabel.setLayoutY(deskOriginY + (deskH / 2) - 12);

        seatCanvas.getChildren().addAll(deskRect, deskLabel);

        List<Seat> seats = seatRepository.findAllByDeskId(currentDesk.getId());

        List<Long> occupiedIds = reservationService.getOccupiedSeatIds(
                currentDesk.getId(),
                globalParams.getSelectedDate(),
                globalParams.getSelectedStartTime(),
                globalParams.getSelectedEndTime());

        for (Seat seat : seats) {
            boolean isOccupied = occupiedIds.contains(seat.getId());
            drawSeatNode(seat, isOccupied);
        }
    }

    private void drawSeatNode(Seat seat, boolean isOccupied) {
        double relX = seat.getRelX() != null ? seat.getRelX() : 0.5;
        double relY = seat.getRelY() != null ? seat.getRelY() : 0.5;

        double deskW = currentDesk.getWidth();
        double deskH = currentDesk.getHeight();

        // Sandalyeyi masanin ETRAFINA konumlandir
        // relX ve relY 0-1 arasinda, 0.5 = ortada
        // Sandalyeler masanin kenarlarinin disinda olmali
        double absX, absY;

        // Masanin kenarlarindan offset (sandalye buyuklugu kadar)
        double offset = SEAT_SIZE + 8;

        // relX ve relY degerlerine gore pozisyon belirle
        // Sol kenar (relX < 0.3)
        if (relX < 0.3) {
            absX = deskOriginX - offset;
            absY = deskOriginY + (relY * deskH) - (SEAT_SIZE / 2);
        }
        // Sag kenar (relX > 0.7)
        else if (relX > 0.7) {
            absX = deskOriginX + deskW + offset - SEAT_SIZE;
            absY = deskOriginY + (relY * deskH) - (SEAT_SIZE / 2);
        }
        // Ust kenar (relY < 0.3)
        else if (relY < 0.3) {
            absX = deskOriginX + (relX * deskW) - (SEAT_SIZE / 2);
            absY = deskOriginY - offset;
        }
        // Alt kenar (relY > 0.7)
        else if (relY > 0.7) {
            absX = deskOriginX + (relX * deskW) - (SEAT_SIZE / 2);
            absY = deskOriginY + deskH + offset - SEAT_SIZE;
        }
        // Ortada ise (fallback - sol kenara koy)
        else {
            absX = deskOriginX - offset;
            absY = deskOriginY + (relY * deskH) - (SEAT_SIZE / 2);
        }

        StackPane seatNode = new StackPane();
        seatNode.setLayoutX(absX);
        seatNode.setLayoutY(absY);
        seatNode.setPrefSize(SEAT_SIZE, SEAT_SIZE);

        Rectangle r = new Rectangle(SEAT_SIZE, SEAT_SIZE);
        r.setArcWidth(10);
        r.setArcHeight(10);

        Label label = new Label(String.valueOf(seat.getSeatNumber()));
        label.setStyle(
                "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 12px; " +
                        "-fx-effect: dropshadow(one-pass-box, black, 2, 0.5, 0, 0);");

        if (isOccupied) {
            // DOLU SEAT
            r.setFill(Color.web("#95a5a6"));
            r.setStroke(Color.web("#7f8c8d"));
            seatNode.setOpacity(0.6);
            seatNode.setCursor(Cursor.DEFAULT);
        } else {
            // BOŞ SEAT (normal hali)
            r.setFill(Color.web("#27ae60"));
            r.setStroke(Color.web("#1e8449"));
            r.setStrokeWidth(1.5);
            r.setEffect(new DropShadow(4, Color.rgb(0, 0, 0, 0.80)));
            seatNode.setCursor(Cursor.HAND);

            // Tıklayınca seçim
            seatNode.setOnMouseClicked(e -> selectSeat(seatNode, seat));

            // Hover efekti (seçili değilse hafif büyüt + yeşil glow)
            seatNode.setOnMouseEntered(e -> {
                if (seatNode != selectedSeatNode) {
                    seatNode.setScaleX(1.08);
                    seatNode.setScaleY(1.08);
                    r.setEffect(new DropShadow(10, Color.web("#27ae60")));
                }
            });

            seatNode.setOnMouseExited(e -> {
                if (seatNode != selectedSeatNode) {
                    seatNode.setScaleX(1.0);
                    seatNode.setScaleY(1.0);
                    r.setStroke(Color.web("#1e8449"));
                    r.setStrokeWidth(1.5);
                    r.setEffect(new DropShadow(4, Color.rgb(0, 0, 0, 0.80)));
                }
            });
        }

        seatNode.getChildren().addAll(r, label);
        seatCanvas.getChildren().add(seatNode);
    }

    private void selectSeat(StackPane node, Seat seat) {
        if (selectedSeatNode != null) {
            Rectangle oldRect = (Rectangle) selectedSeatNode.getChildren().get(0);
            oldRect.setStroke(Color.web("#1e8449"));
            oldRect.setStrokeWidth(1.5);
            oldRect.setEffect(new DropShadow(4, Color.rgb(0, 0, 0, 0.80)));
            selectedSeatNode.setScaleX(1.0);
            selectedSeatNode.setScaleY(1.0);
        }

        selectedSeatNode = node;
        Rectangle rect = (Rectangle) node.getChildren().get(0);
        rect.setStroke(Color.GOLD);
        rect.setStrokeWidth(3);
        rect.setEffect(new DropShadow(10, Color.GOLD));

        globalParams.setSelectedSeat(seat);
        showConfirmOverlay();
    }

    /** Normal onay modu */
    private void showConfirmOverlay() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        lblConfirmDate.setText(globalParams.getSelectedDate().format(dateFormatter));
        lblConfirmTime.setText(
                globalParams.getSelectedStartTime() + " – " + globalParams.getSelectedEndTime());

        Facility facility = globalParams.getSelectedFacility();
        FacilityBlock block = globalParams.getSelectedFacilityBlock();
        Desk desk = currentDesk;
        Seat seat = globalParams.getSelectedSeat();

        lblConfirmFacility.setText(facility != null ? facility.getName() : "-");
        lblConfirmBlock.setText(block != null ? block.getName() : "-");
        lblConfirmDesk.setText(desk.getIdRange() != null ? desk.getIdRange() : ("Masa " + desk.getId()));
        lblConfirmSeat.setText("Koltuk #" + (seat != null ? seat.getSeatNumber() : "?"));

        // Modal'ı "confirm" moduna al
        lblModalTitle.setText("Rezervasyonu Onayla");

        lblWarningMessage.setVisible(true);
        lblWarningMessage.setManaged(true);

        lblErrorMessage.setVisible(false);
        lblErrorMessage.setManaged(false);

        lblSuccessMessage.setVisible(false);
        lblSuccessMessage.setManaged(false);

        confirmButtonsRow.setVisible(true);
        confirmButtonsRow.setManaged(true);

        errorButtonsRow.setVisible(false);
        errorButtonsRow.setManaged(false);

        successButtonsRow.setVisible(false);
        successButtonsRow.setManaged(false);

        confirmOverlay.setVisible(true);
        mainLayout.setEffect(new GaussianBlur(5));
    }

    /** Hata durumunda: onay ekranını hata ekranına çevirir */
    private void showErrorOverlay(String errorMessage) {
        lblModalTitle.setText("Rezervasyon Basarisiz");

        // 30 dk uyarısını gizle
        lblWarningMessage.setVisible(false);
        lblWarningMessage.setManaged(false);

        // success mesajını gizle
        lblSuccessMessage.setVisible(false);
        lblSuccessMessage.setManaged(false);

        // Trigger hatalarını kullanıcı dostu mesaja çevir
        String userFriendlyMessage;
        if (errorMessage.contains("TRIGGER_ERROR:MAX_RESERVATION_LIMIT")) {
            userFriendlyMessage = "Maksimum Rezervasyon Limiti Asildi\n\n" +
                    "Aynı anda en fazla 3 aktif rezervasyonunuz olabilir.\n" +
                    "Mevcut rezervasyonlarınızdan birini iptal ettikten sonra yeni rezervasyon yapabilirsiniz.";
        } else if (errorMessage.contains("TRIGGER_ERROR:TIME_CONFLICT")) {
            userFriendlyMessage = "Zaman Cakismasi\n\n" +
                    "Seçilen zaman aralığında zaten bir rezervasyonunuz bulunmaktadır.\n" +
                    "Lütfen farklı bir zaman dilimi seçin.";
        } else if (errorMessage.contains("reservation") && errorMessage.contains("conflict")) {
            userFriendlyMessage = "Koltuk Alinmis\n\n" +
                    "Seçtiğiniz koltuk başka bir kullanıcı tarafından az önce rezerve edildi.\n" +
                    "Lütfen başka bir koltuk seçin.";
        } else {
            userFriendlyMessage = "Rezervasyon Olusturulamadi\n\n" + errorMessage;
        }

        lblErrorMessage.setText(userFriendlyMessage);
        lblErrorMessage.setVisible(true);
        lblErrorMessage.setManaged(true);

        // Confirm/Cancel yerine sadece OK butonu
        confirmButtonsRow.setVisible(false);
        confirmButtonsRow.setManaged(false);

        errorButtonsRow.setVisible(true);
        errorButtonsRow.setManaged(true);

        successButtonsRow.setVisible(false);
        successButtonsRow.setManaged(false);

        confirmOverlay.setVisible(true);
        mainLayout.setEffect(new GaussianBlur(5));
    }

    /** Başarılı rezervasyon sonrası info modu */
    private void showSuccessOverlay() {
        lblModalTitle.setText("Rezervasyon Olusturuldu");

        // Confirm / Error / Warning kapat
        lblWarningMessage.setVisible(false);
        lblWarningMessage.setManaged(false);

        lblErrorMessage.setVisible(false);
        lblErrorMessage.setManaged(false);

        // Başarı mesajı
        lblSuccessMessage.setText("✅ Rezervasyonunuz basariyla olusturuldu.");
        lblSuccessMessage.setVisible(true);
        lblSuccessMessage.setManaged(true);

        confirmButtonsRow.setVisible(false);
        confirmButtonsRow.setManaged(false);

        errorButtonsRow.setVisible(false);
        errorButtonsRow.setManaged(false);

        successButtonsRow.setVisible(true);
        successButtonsRow.setManaged(true);

        confirmOverlay.setVisible(true);
        mainLayout.setEffect(new GaussianBlur(5));
    }

    @FXML
    public void hideOverlay() {
        // Cancel: sadece modal kapanır, seçim de sıfırlanır
        confirmOverlay.setVisible(false);
        mainLayout.setEffect(null);

        if (selectedSeatNode != null) {
            Rectangle rect = (Rectangle) selectedSeatNode.getChildren().get(0);
            rect.setStroke(Color.web("#1e8449"));
            rect.setStrokeWidth(1.5);
            rect.setEffect(new DropShadow(4, Color.rgb(0, 0, 0, 0.80)));
            selectedSeatNode.setScaleX(1.0);
            selectedSeatNode.setScaleY(1.0);
            selectedSeatNode = null;
        }
    }

    /** Hata ekranındaki OK butonu */
    @FXML
    public void handleErrorOk() {
        confirmOverlay.setVisible(false);
        mainLayout.setEffect(null);
        // Seçimi sıfırla ve ekranı güncel dolulukla yeniden çiz
        selectedSeatNode = null;
        renderScene();
    }

    /** Başarılı rezervasyon sonrası "Back to Home" */
    @FXML
    public void handleSuccessHome() {
        stopRefreshTimer();
        confirmOverlay.setVisible(false);
        mainLayout.setEffect(null);
        userHomeController.showDashboard();
    }

    @FXML
    public void handleConfirm() {
        try {
            reservationService.createReservation(
                    globalParams.getSelectedSeat().getId(),
                    globalParams.getSelectedDate(),
                    globalParams.getSelectedStartTime(),
                    globalParams.getSelectedEndTime());
            stopRefreshTimer();
            // Dashboard'a gitmek yerine success info ekranı
            showSuccessOverlay();
        } catch (Exception e) {
            // Hata mesajını göster - trigger hatalarını kullanıcı dostu mesaja çevirir
            showErrorOverlay(e.getMessage());
        }
    }

    @FXML
    public void handleBack() {
        stopRefreshTimer();
        userHomeController.setView("/fxml/user/reservation/user-reservation-step3.fxml");
    }
}
