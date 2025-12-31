package com.studyflow.app.gui.user.reservation;

import com.studyflow.app.context.UserSessionContext;
import com.studyflow.app.gui.user.UserHomeController;
import com.studyflow.app.model.reservation.Reservation;
import com.studyflow.app.service.reservation.ReservationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
public class UserReservationsController {

    @FXML
    private Label userNameLabel;

    // Toggle Butonları
    @FXML
    private ToggleButton btnActive;
    @FXML
    private ToggleButton btnExpired;
    @FXML
    private ToggleGroup viewToggleGroup;

    // Liste Konteynırları
    @FXML
    private ScrollPane scrollActive;
    @FXML
    private ScrollPane scrollExpired;
    @FXML
    private VBox activeContainer;
    @FXML
    private VBox pastContainer;

    @Autowired
    private ReservationService reservationService;
    @Autowired
    private UserSessionContext userSessionContext;
    @Autowired
    private UserHomeController userHomeController;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        if (userSessionContext.getCurrentUser() != null) {
            userNameLabel.setText(
                    userSessionContext.getCurrentUser().getFirstName() + " " +
                            userSessionContext.getCurrentUser().getLastName());
        }

        // Listeleri yükle
        loadActiveReservations();
        loadPastReservations();

        // Toggle Dinleyicisi (Geçiş Mantığı)
        viewToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                // Kullanıcı seçili butona tekrar basarsa seçim kalkmasın
                oldVal.setSelected(true);
                return;
            }

            if (newVal == btnActive) {
                showActiveView();
            } else {
                showExpiredView();
            }
        });

        // Varsayılan görünüm
        showActiveView();
    }

    private void showActiveView() {
        scrollActive.setVisible(true);
        scrollExpired.setVisible(false);
    }

    private void showExpiredView() {
        scrollActive.setVisible(false);
        scrollExpired.setVisible(true);
    }

    /* ------------------ UPCOMING ------------------ */
    private void loadActiveReservations() {
        activeContainer.getChildren().clear();
        List<Reservation> list = reservationService.getCurrentUserActiveReservations();

        if (list == null || list.isEmpty()) {
            activeContainer.getChildren().add(createEmptyState("Yaklasan rezervasyon yok."));
            return;
        }
        for (Reservation r : list) {
            activeContainer.getChildren().add(createReservationCard(r, true));
        }
    }

    /* ------------------ HISTORY ------------------ */
    private void loadPastReservations() {
        pastContainer.getChildren().clear();
        List<Reservation> list = reservationService.getCurrentUserPastReservations();

        if (list == null || list.isEmpty()) {
            pastContainer.getChildren().add(createEmptyState("Gecmis rezervasyon yok."));
            return;
        }
        for (Reservation r : list) {
            pastContainer.getChildren().add(createReservationCard(r, false));
        }
    }

    /* ------------------ CARD FACTORY ------------------ */
    private VBox createReservationCard(Reservation r, boolean isActive) {
        VBox card = new VBox(0);
        card.getStyleClass().add("ticket-card");
        card.getStyleClass().add(isActive ? "ticket-card-active" : "ticket-card-past");
        card.setMaxWidth(750);
        card.setMinWidth(700);

        // HEADER
        HBox header = new HBox(12);
        header.getStyleClass().add("ticket-header");
        header.setAlignment(Pos.CENTER_LEFT);

        String facilityName = (r.getFacility() != null) ? r.getFacility().getName() : "Bilinmeyen Tesis";
        Label lblFacility = new Label(facilityName);
        lblFacility.setStyle("-fx-font-weight: 800; -fx-font-size: 18px; -fx-text-fill: #111827;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblDate = new Label(r.getReservationDate().format(dateFormatter));
        lblDate.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280;");

        header.getChildren().addAll(lblFacility, spacer, lblDate);

        // BODY
        HBox body = new HBox(25);
        body.setPadding(new Insets(15, 20, 15, 20));
        body.setAlignment(Pos.CENTER_LEFT);

        // TIME
        VBox timeBox = new VBox(4);
        Label lblTimeTitle = new Label("ZAMAN");
        lblTimeTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #9ca3af;");
        Label lblTimeVal = new Label(
                r.getStartTime().format(timeFormatter) + " - " + r.getEndTime().format(timeFormatter));
        lblTimeVal.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #111827;");
        timeBox.getChildren().addAll(lblTimeTitle, lblTimeVal);

        // LOCATION
        VBox locBox = new VBox(4);
        Label lblLocTitle = new Label("CALISMA ALANI");
        lblLocTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #9ca3af;");

        String blockName = (r.getFacilityBlock() != null) ? r.getFacilityBlock().getName() : "-";

        String deskText = "-";
        if (r.getDesk() != null) {
            if (r.getDesk().getIdRange() != null && !r.getDesk().getIdRange().isBlank()) {
                deskText = "Masa " + r.getDesk().getIdRange();
            } else {
                deskText = "Masa " + r.getDesk().getId();
            }
        } else if (r.getSeat() != null && r.getSeat().getDesk() != null) {
            if (r.getSeat().getDesk().getIdRange() != null && !r.getSeat().getDesk().getIdRange().isBlank()) {
                deskText = "Masa " + r.getSeat().getDesk().getIdRange();
            } else {
                deskText = "Masa " + r.getSeat().getDesk().getId();
            }
        }

        String seatNum = (r.getSeat() != null) ? String.valueOf(r.getSeat().getSeatNumber()) : "?";

        Label lblLocMain = new Label(blockName + " - " + deskText + " - Koltuk #" + seatNum);
        lblLocMain.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #111827;");
        locBox.getChildren().addAll(lblLocTitle, lblLocMain);

        // CHIP ve BUTONLAR
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        VBox actionBox = new VBox(8);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        // Status Label
        String statusText = isActive ? "AKTIF" : (r.getStatus() != null ? r.getStatus() : "GECMIS");
        Label lblStatus = new Label(statusText);
        lblStatus.getStyleClass().add(isActive ? "chip-active" : "chip-past");
        if ("CANCELLED".equals(r.getStatus())) {
            lblStatus.getStyleClass().clear();
            lblStatus.getStyleClass().add("chip-cancelled");
            lblStatus.setStyle(
                    "-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-padding: 4 12; -fx-background-radius: 20; -fx-font-weight: 700;");
        }

        actionBox.getChildren().add(lblStatus);

        // Aktif rezervasyonlar için iptal ve güncelleme butonları - HER ZAMAN GÖSTER
        if (isActive) {
            HBox buttonBox = new HBox(8);
            buttonBox.setAlignment(Pos.CENTER_RIGHT);

            // Güncelle butonu
            Button btnUpdate = new Button("Guncelle");
            btnUpdate.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-size: 12px; " +
                    "-fx-padding: 6 12; -fx-background-radius: 6; -fx-cursor: hand;");
            btnUpdate.setOnAction(e -> {
                if (r.isCancellable()) {
                    showUpdateDialog(r);
                } else {
                    showErrorAlert("Guncelleme Yapilamaz",
                            "Rezervasyon baslangicina 1 saatten az kaldigi icin guncelleme yapilamaz.\n\n" +
                                    "Rezervasyon baslangic zamani: " + r.getReservationDate() + " " + r.getStartTime());
                }
            });

            // İptal butonu
            Button btnCancel = new Button("Iptal Et");
            btnCancel.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 12px; " +
                    "-fx-padding: 6 12; -fx-background-radius: 6; -fx-cursor: hand;");
            btnCancel.setOnAction(e -> {
                if (r.isCancellable()) {
                    showCancelDialog(r);
                } else {
                    showErrorAlert("Iptal Yapilamaz",
                            "Rezervasyon baslangicina 1 saatten az kaldigi icin iptal edilemez.\n\n" +
                                    "Rezervasyon baslangic zamani: " + r.getReservationDate() + " " + r.getStartTime());
                }
            });

            buttonBox.getChildren().addAll(btnUpdate, btnCancel);
            actionBox.getChildren().add(buttonBox);

            // İptal için kalan süre bilgisi
            if (r.isCancellable()) {
                String deadlineInfo = reservationService.getCancellationDeadlineInfo(r.getId());
                Label lblDeadline = new Label(deadlineInfo);
                lblDeadline.setStyle("-fx-font-size: 11px; -fx-text-fill: #10B981;");
                actionBox.getChildren().add(lblDeadline);
            } else {
                Label lblNoCancel = new Label("Iptal/guncelleme suresi gecti (1 saat kurali)");
                lblNoCancel.setStyle("-fx-font-size: 11px; -fx-text-fill: #EF4444;");
                actionBox.getChildren().add(lblNoCancel);
            }
        }

        body.getChildren().addAll(timeBox, locBox, spacer2, actionBox);
        card.getChildren().addAll(header, body);
        return card;
    }

    /* ------------------ İPTAL DİALOGU ------------------ */
    private void showCancelDialog(Reservation reservation) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Rezervasyon Iptali");
        dialog.setHeaderText("Rezervasyonu iptal etmek istediginize emin misiniz?");
        dialog.initModality(Modality.APPLICATION_MODAL);

        // Dialog içeriği
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");

        // Rezervasyon bilgileri
        Label infoLabel = new Label(
                reservation.getFacility().getName() + "\n" +
                        "📅 " + reservation.getReservationDate().format(dateFormatter) + "\n" +
                        "⏰ " + reservation.getStartTime().format(timeFormatter) + " - "
                        + reservation.getEndTime().format(timeFormatter));
        infoLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151;");

        // İptal nedeni
        Label reasonLabel = new Label("Iptal Nedeni (Opsiyonel):");
        reasonLabel.setStyle("-fx-font-weight: 600;");
        TextArea reasonField = new TextArea();
        reasonField.setPromptText("Iptal nedeninizi yazin...");
        reasonField.setPrefRowCount(3);
        reasonField.setMaxWidth(400);

        // Uyarı
        Label warningLabel = new Label("Bu islem geri alinamaz!");
        warningLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: 600;");

        content.getChildren().addAll(infoLabel, reasonLabel, reasonField, warningLabel);
        dialog.getDialogPane().setContent(content);

        // Butonlar
        ButtonType cancelButtonType = new ButtonType("Iptal Et", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeButtonType = new ButtonType("Vazgec", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(cancelButtonType, closeButtonType);

        // İptal butonu stillendir
        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(cancelButtonType);
        cancelBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white;");

        dialog.setResultConverter(buttonType -> {
            if (buttonType == cancelButtonType) {
                return reasonField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(reason -> {
            try {
                String message = reservationService.cancelReservation(reservation.getId(), reason);
                showSuccessAlert("Iptal Basarili", message);
                // Listeyi yenile
                loadActiveReservations();
                loadPastReservations();
            } catch (Exception e) {
                showErrorAlert("Iptal Hatasi", e.getMessage());
            }
        });
    }

    /* ------------------ GÜNCELLEME DİALOGU ------------------ */
    private void showUpdateDialog(Reservation reservation) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Rezervasyon Guncelleme");
        dialog.setHeaderText("Rezervasyon zamanini guncelleyin");
        dialog.initModality(Modality.APPLICATION_MODAL);

        // Dialog icerigi
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");

        // Mevcut bilgiler
        Label currentLabel = new Label("Mevcut Rezervasyon:");
        currentLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 14px;");

        Label currentInfo = new Label(
                "Yer: " + reservation.getFacility().getName() + " - " + reservation.getFacilityBlock().getName() + "\n"
                        +
                        "Tarih: " + reservation.getReservationDate().format(dateFormatter) + "\n" +
                        "Saat: " + reservation.getStartTime().format(timeFormatter) + " - "
                        + reservation.getEndTime().format(timeFormatter));
        currentInfo.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151;");

        // Yeni tarih secimi
        Label dateLabel = new Label("Yeni Tarih:");
        dateLabel.setStyle("-fx-font-weight: 600;");
        DatePicker datePicker = new DatePicker(reservation.getReservationDate());
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });

        // Yeni saat secimi
        HBox timeBox = new HBox(15);

        VBox startBox = new VBox(5);
        Label startLabel = new Label("Baslangic:");
        startLabel.setStyle("-fx-font-weight: 600;");
        ComboBox<String> startTimeCombo = new ComboBox<>();
        startBox.getChildren().addAll(startLabel, startTimeCombo);

        VBox endBox = new VBox(5);
        Label endLabel = new Label("Bitis (1-3 saat):");
        endLabel.setStyle("-fx-font-weight: 600;");
        ComboBox<String> endTimeCombo = new ComboBox<>();
        endBox.getChildren().addAll(endLabel, endTimeCombo);

        // Tarih degistiginde baslangic saatlerini guncelle
        datePicker.setOnAction(e -> {
            LocalDate selectedDate = datePicker.getValue();
            updateStartTimeComboForDate(startTimeCombo, selectedDate);
            // Baslangic saati degisince bitis de guncellensin
            if (startTimeCombo.getValue() != null) {
                updateEndTimeComboForUpdate(endTimeCombo, startTimeCombo.getValue());
            }
        });

        // Baslangic saati secildiginde bitis saatlerini guncelle (1-3 saat araliginda)
        startTimeCombo.setOnAction(e -> {
            String selectedStart = startTimeCombo.getValue();
            if (selectedStart != null) {
                updateEndTimeComboForUpdate(endTimeCombo, selectedStart);
            }
        });

        // Ilk yukleme - mevcut tarihe gore baslangic saatlerini ayarla
        updateStartTimeComboForDate(startTimeCombo, reservation.getReservationDate());
        startTimeCombo.setValue(reservation.getStartTime().format(timeFormatter));
        updateEndTimeComboForUpdate(endTimeCombo, reservation.getStartTime().format(timeFormatter));
        endTimeCombo.setValue(reservation.getEndTime().format(timeFormatter));

        timeBox.getChildren().addAll(startBox, endBox);

        // Uyari
        Label noteLabel = new Label(
                "Not: Guncelleme ayni koltuk icin yapilir. Farkli koltuk icin yeni rezervasyon olusturun.\nSure: Minimum 1, maksimum 3 saat.");
        noteLabel.setStyle("-fx-text-fill: #3B82F6; -fx-font-size: 12px;");
        noteLabel.setWrapText(true);
        noteLabel.setMaxWidth(380);

        content.getChildren().addAll(currentLabel, currentInfo, dateLabel, datePicker, timeBox, noteLabel);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(420);

        // Butonlar
        ButtonType updateButtonType = new ButtonType("Guncelle", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeButtonType = new ButtonType("Vazgec", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, closeButtonType);

        // Guncelle butonu stillendir
        Button updateBtn = (Button) dialog.getDialogPane().lookupButton(updateButtonType);
        updateBtn.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white;");

        dialog.setResultConverter(buttonType -> buttonType == updateButtonType);

        Optional<Boolean> result = dialog.showAndWait();
        if (result.isPresent() && result.get()) {
            try {
                LocalDate newDate = datePicker.getValue();
                LocalTime newStartTime = LocalTime.parse(startTimeCombo.getValue(), timeFormatter);
                LocalTime newEndTime = LocalTime.parse(endTimeCombo.getValue(), timeFormatter);

                // Gecmis zaman kontrolu
                LocalDateTime newDateTime = LocalDateTime.of(newDate, newStartTime);
                if (newDateTime.isBefore(LocalDateTime.now())) {
                    showErrorAlert("Gecersiz Zaman", "Gecmis bir zamana rezervasyon guncellenemez.");
                    return;
                }

                // Validasyon - sure kontrolu (1-3 saat)
                long durationMinutes = java.time.Duration.between(newStartTime, newEndTime).toMinutes();
                if (durationMinutes < 60) {
                    showErrorAlert("Gecersiz Sure", "Rezervasyon suresi en az 1 saat olmalidir.");
                    return;
                }
                if (durationMinutes > 180) {
                    showErrorAlert("Gecersiz Sure", "Rezervasyon suresi en fazla 3 saat olabilir.");
                    return;
                }
                if (newEndTime.isBefore(newStartTime) || newEndTime.equals(newStartTime)) {
                    showErrorAlert("Gecersiz Saat", "Bitis saati baslangic saatinden sonra olmalidir.");
                    return;
                }

                String message = reservationService.updateReservation(reservation.getId(), newDate, newStartTime,
                        newEndTime);
                showSuccessAlert("Guncelleme Basarili", message);
                // Listeyi yenile
                loadActiveReservations();
            } catch (Exception e) {
                showErrorAlert("Guncelleme Hatasi", e.getMessage());
            }
        }
    }

    /**
     * Guncelleme dialogu icin bitis saati seceneklerini doldurur (1-3 saat
     * araliginda)
     */
    private void updateEndTimeComboForUpdate(ComboBox<String> endCombo, String startTimeStr) {
        endCombo.getItems().clear();

        try {
            LocalTime startTime = LocalTime.parse(startTimeStr, timeFormatter);
            LocalTime maxEndTime = LocalTime.of(23, 0); // Tesis kapanisi

            // 1 saat sonradan 3 saat sonraya kadar (30 dakika aralikla)
            for (int addMinutes = 60; addMinutes <= 180; addMinutes += 30) {
                LocalTime endTime = startTime.plusMinutes(addMinutes);
                if (!endTime.isAfter(maxEndTime)) {
                    endCombo.getItems().add(endTime.format(timeFormatter));
                }
            }

            // Varsayilan olarak 2 saat sonrasini sec
            LocalTime defaultEnd = startTime.plusHours(2);
            if (endCombo.getItems().contains(defaultEnd.format(timeFormatter))) {
                endCombo.setValue(defaultEnd.format(timeFormatter));
            } else if (!endCombo.getItems().isEmpty()) {
                endCombo.setValue(endCombo.getItems().get(0));
            }
        } catch (Exception e) {
            // Hata durumunda bos birak
        }
    }

    /**
     * Tarihe gore baslangic saatlerini doldurur.
     * Bugun seciliyse sadece gelecek saatler gosterilir.
     */
    private void updateStartTimeComboForDate(ComboBox<String> combo, LocalDate selectedDate) {
        combo.getItems().clear();

        LocalTime now = LocalTime.now();
        boolean isToday = selectedDate.equals(LocalDate.now());

        for (int hour = 8; hour <= 22; hour++) {
            LocalTime time = LocalTime.of(hour, 0);
            // Bugun ise sadece gelecek saatleri ekle (en az 1 saat sonrasi)
            if (!isToday || time.isAfter(now.plusHours(1))) {
                combo.getItems().add(String.format("%02d:00", hour));
            }

            if (hour < 22) {
                LocalTime halfTime = LocalTime.of(hour, 30);
                if (!isToday || halfTime.isAfter(now.plusHours(1))) {
                    combo.getItems().add(String.format("%02d:30", hour));
                }
            }
        }

        // Varsayilan olarak ilk secenegi sec
        if (!combo.getItems().isEmpty()) {
            combo.setValue(combo.getItems().get(0));
        }
    }

    private void populateTimeCombo(ComboBox<String> combo) {
        for (int hour = 8; hour <= 22; hour++) {
            combo.getItems().add(String.format("%02d:00", hour));
            if (hour < 22) {
                combo.getItems().add(String.format("%02d:30", hour));
            }
        }
    }

    /* ------------------ ALERT HELPERS ------------------ */
    private void showSuccessAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showErrorAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private Label createEmptyState(String msg) {
        Label l = new Label(msg);
        l.getStyleClass().add("reservation-empty-label");
        l.setMaxWidth(Double.MAX_VALUE);
        l.setAlignment(Pos.CENTER);
        return l;
    }

    @FXML
    public void handleBack() {
        userHomeController.showDashboard();
    }
}