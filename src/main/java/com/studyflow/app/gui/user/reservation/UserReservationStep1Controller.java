package com.studyflow.app.gui.user.reservation;

import com.studyflow.app.context.GlobalParamsContext;
import com.studyflow.app.gui.NavigationService;
import com.studyflow.app.gui.ViewFactory;
import com.studyflow.app.gui.user.UserHomeController;
import com.studyflow.app.model.facility.DailySchedule;
import com.studyflow.app.model.facility.Facility;
import com.studyflow.app.service.reservation.ReservationService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class UserReservationStep1Controller {

    // GÜN SEÇİMİ & HAFTALIK TAKVİM
    @FXML
    private BorderPane mainLayout;
    @FXML
    private HBox dayContainer;
    @FXML
    private VBox weeklyScheduleInfoBox;
    @FXML
    private Label errorLabel;

    // Time Picker kutularındaki label'lar
    @FXML
    private Label startTimeDisplay;
    @FXML
    private Label endTimeDisplay;

    // Overlay (büyük saat seçici)
    @FXML
    private VBox timeOverlay;
    @FXML
    private Label overlayTitleLabel;
    @FXML
    private VBox overlayTimesBox;

    // YENİ: Öneri Kutusu
    @FXML
    private HBox suggestionBox;
    @FXML
    private Label lblSuggestionText;

    @Autowired
    private GlobalParamsContext globalParams;
    @Autowired
    private NavigationService navigationService;
    @Autowired
    private ViewFactory viewFactory;
    @Autowired
    private UserHomeController userHomeController;
    @Autowired
    private ReservationService reservationService; // Servis Eklendi

    private Facility facility;
    private ToggleGroup dayGroup;
    private LocalDate selectedDate;

    private enum TimeMode {
        START, END
    }

    private TimeMode currentMode;

    private final List<LocalTime> startOptions = new ArrayList<>();
    private final List<LocalTime> endOptions = new ArrayList<>();

    private LocalTime selectedStartTime;
    private LocalTime selectedEndTime;

    // Öneri verisi
    private LocalTime suggestedStart;
    private LocalTime suggestedEnd;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        facility = globalParams.getSelectedFacility();
        if (facility == null) {
            return;
        }

        dayGroup = new ToggleGroup();
        renderDays();
        renderModernWeeklySchedule();

        // Gün seçilince
        dayGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                LocalDate date = (LocalDate) newVal.getUserData();
                handleDateSelection(date);
            }
        });

        if (!dayContainer.getChildren().isEmpty()) {
            ((ToggleButton) dayContainer.getChildren().get(0)).setSelected(true);
        }

        hideTimeOverlay();
    }

    /* -------------------- GÜN KARTLARI (AYNI) -------------------- */

    private void renderDays() {
        dayContainer.getChildren().clear();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            DailySchedule schedule = facility.getWeeklyCalendar().getScheduleForDay(dayOfWeek);
            boolean isOpen = schedule != null && (schedule.getIsClosed() == null || !schedule.getIsClosed());
            ToggleButton btn = createDayButton(date, isOpen);
            dayContainer.getChildren().add(btn);
        }
    }

    private ToggleButton createDayButton(LocalDate date, boolean isOpen) {
        ToggleButton btn = new ToggleButton();
        btn.setPrefSize(120, 80); // Daha kucuk boyut - 7 gun yan yana sigsin
        btn.setMinSize(100, 70);
        btn.setMaxSize(140, 90);
        btn.setToggleGroup(dayGroup);
        btn.setUserData(date);
        btn.getStyleClass().add("day-card");

        // Kisa gun adi (Mon, Tue vs.)
        String dayName = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        if (date.equals(LocalDate.now()))
            dayName = "Bugun";

        String dateText = date.getDayOfMonth() + " " + date.getMonth().toString().substring(0, 3);

        VBox content = new VBox(3);
        content.setAlignment(Pos.CENTER);
        Label lblDay = new Label(dayName);
        lblDay.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #333;");
        Label lblDate = new Label(dateText);
        lblDate.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        if (!isOpen) {
            btn.setDisable(true);
            Label lblClosed = new Label("KAPALI");
            lblClosed.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 10px;");
            content.getChildren().addAll(lblDay, lblDate, lblClosed);
            btn.setStyle("-fx-opacity: 0.5; -fx-background-color: #f5f5f5;");
        } else {
            content.getChildren().addAll(lblDay, lblDate);
        }

        btn.setGraphic(content);
        return btn;
    }

    private void renderModernWeeklySchedule() {
        weeklyScheduleInfoBox.getChildren().clear();
        if (facility.getWeeklyCalendar() == null)
            return;
        List<DailySchedule> sortedDays = new ArrayList<>(facility.getWeeklyCalendar().getDays());
        sortedDays.sort(Comparator.comparing(DailySchedule::getDayOfWeek));

        for (DailySchedule ds : sortedDays) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setSpacing(10);
            row.setStyle("-fx-border-color: #eee; -fx-border-width: 0 0 1 0; -fx-padding: 8 0;");
            Label lblDay = new Label(ds.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            lblDay.setPrefWidth(50);
            lblDay.setStyle("-fx-font-weight: bold; -fx-text-fill: #555; -fx-font-size: 16px;");
            Label lblTime = new Label();
            lblTime.setStyle("-fx-font-size: 16px; -fx-font-family: 'Segoe UI';");
            boolean isClosed = ds.getIsClosed() != null && ds.getIsClosed();
            if (isClosed) {
                lblTime.setText("Kapali");
                lblTime.setStyle(lblTime.getStyle() + "-fx-text-fill: #e74c3c;");
            } else {
                lblTime.setText(ds.getOpenTime() + " - " + ds.getCloseTime());
                lblTime.setStyle(lblTime.getStyle() + "-fx-text-fill: #27ae60;");
            }
            row.getChildren().addAll(lblDay, lblTime);
            weeklyScheduleInfoBox.getChildren().add(row);
        }
    }

    /* -------------------- SAAT SEÇİMİ -------------------- */

    private void handleDateSelection(LocalDate date) {
        this.selectedDate = date;
        hideTimeOverlay();
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Öneriyi sıfırla
        if (suggestionBox != null) {
            suggestionBox.setVisible(false);
            suggestionBox.setManaged(false);
        }

        startOptions.clear();
        endOptions.clear();
        selectedStartTime = null;
        selectedEndTime = null;
        startTimeDisplay.setText("Sec");
        endTimeDisplay.setText("Sec");

        DailySchedule schedule = facility.getWeeklyCalendar().getScheduleForDay(date.getDayOfWeek());
        boolean isClosed = schedule == null || (schedule.getIsClosed() != null && schedule.getIsClosed());
        if (isClosed)
            return;

        LocalTime openTime = schedule.getOpenTime();
        LocalTime closeTime = schedule.getCloseTime();
        LocalTime now = LocalTime.now();

        LocalTime loopStart = openTime;
        if (date.equals(LocalDate.now()) && now.isAfter(openTime)) {
            int minute = now.getMinute();
            int remainder = 30 - (minute % 30);
            loopStart = now.plusMinutes(remainder).withSecond(0).withNano(0);
            if (loopStart.isAfter(closeTime) || loopStart.equals(closeTime)) {
                showError("Tesis gunun geri kalaninda kapali.");
                return;
            }
        }

        LocalTime t = loopStart;
        while (t.isBefore(closeTime)) {
            startOptions.add(t);
            LocalTime nextT = t.plusMinutes(30);
            if (nextT.isBefore(t))
                break;
            t = nextT;
        }

        // YENİ: Öneri Hesapla
        checkAndShowRecommendation(date);
    }

    // YENİ: Öneri Kontrolü
    private void checkAndShowRecommendation(LocalDate date) {
        String recommendation = reservationService.findBestTimeSuggestion(facility, date);
        if (recommendation != null && suggestionBox != null) {
            try {
                String[] parts = recommendation.split("-");
                suggestedStart = LocalTime.parse(parts[0]);
                suggestedEnd = LocalTime.parse(parts[1]);

                lblSuggestionText.setText("En az yogun saat: " + recommendation);
                suggestionBox.setVisible(true);
                suggestionBox.setManaged(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // YENİ: Öneriyi Uygula Butonu
    @FXML
    public void handleApplySuggestion() {
        if (suggestedStart != null && suggestedEnd != null) {
            // Değerleri doğrudan set et
            selectedStartTime = suggestedStart;
            selectedEndTime = suggestedEnd;

            // Ekranı güncelle
            updateStartDisplay();
            updateEndDisplay();

            // End Options listesini arka planda güncelle (Tutarlılık için)
            DailySchedule schedule = facility.getWeeklyCalendar().getScheduleForDay(selectedDate.getDayOfWeek());
            if (schedule != null) {
                updateEndTimeOptions(selectedStartTime, schedule.getCloseTime());
            }
        }
    }

    // Sabit: Minimum 1 saat, maksimum 3 saat rezervasyon süresi
    private static final int MIN_RESERVATION_HOURS = 1;
    private static final int MAX_RESERVATION_HOURS = 3;

    private void updateEndTimeOptions(LocalTime start, LocalTime limit) {
        endOptions.clear();

        // Minimum 1 saat sonrası (60 dakika)
        LocalTime minEndTime = start.plusHours(MIN_RESERVATION_HOURS);
        // Maksimum 3 saat sonrası (180 dakika)
        LocalTime maxEndTime = start.plusHours(MAX_RESERVATION_HOURS);

        // Kapanış saatini aşmamak için kontrol
        if (maxEndTime.isAfter(limit)) {
            maxEndTime = limit;
        }

        // Sadece 1, 1.5, 2, 2.5 ve 3 saatlik seçenekleri ekle
        LocalTime t = minEndTime;
        while (!t.isAfter(maxEndTime) && !t.isAfter(limit)) {
            endOptions.add(t);
            LocalTime nextT = t.plusMinutes(30);
            // Overflow kontrolü
            if (nextT.isBefore(t))
                break;
            t = nextT;
        }

        // Seçili bitiş zamanı artık geçerli değilse sıfırla
        if (selectedEndTime != null && !endOptions.contains(selectedEndTime)) {
            selectedEndTime = null;
            updateEndDisplay();
        }
    }

    /* -------------------- OVERLAY (AYNI) -------------------- */
    // Overlay metodları ve buton aksiyonları (openStartTimeOverlay, handleNext vb.)
    // senin attığın kodla birebir aynı kalacak.
    // Sadece handleApplySuggestion eklendi.

    @FXML
    public void openStartTimeOverlay() {
        if (startOptions.isEmpty() || selectedDate == null)
            return;
        currentMode = TimeMode.START;
        overlayTitleLabel.setText("Baslangic Saati Secin");
        populateOverlayTimes(startOptions, selectedStartTime);
        showTimeOverlay();
    }

    @FXML
    public void openEndTimeOverlay() {
        if (endOptions.isEmpty() || selectedDate == null)
            return;
        currentMode = TimeMode.END;
        overlayTitleLabel.setText("Bitis Saati Secin");
        populateOverlayTimes(endOptions, selectedEndTime);
        showTimeOverlay();
    }

    private void populateOverlayTimes(List<LocalTime> options, LocalTime current) {
        overlayTimesBox.getChildren().clear();
        double CARD_WIDTH = 360;
        for (LocalTime time : options) {
            Button btn = new Button(time.format(timeFormatter));
            btn.setPrefHeight(88);
            btn.setPrefWidth(CARD_WIDTH);
            btn.setMaxWidth(CARD_WIDTH);
            String baseStyle = "-fx-font-size: 22px; -fx-font-weight: 600; -fx-background-radius: 16; -fx-background-color: linear-gradient(to bottom, #ffffff, #f5f7fa); -fx-text-fill: #2c3e50; -fx-border-radius: 16; -fx-border-color: #d0d4da; -fx-border-width: 1; -fx-padding: 16 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 6, 0, 0, 1);";
            String selectedStyle = "-fx-font-size: 22px; -fx-font-weight: 700; -fx-background-radius: 18; -fx-background-color: linear-gradient(to bottom, #4a90e2, #357abd); -fx-text-fill: white; -fx-border-radius: 18; -fx-border-color: #2f6fbd; -fx-border-width: 2; -fx-padding: 18 22; -fx-effect: dropshadow(three-pass-box, rgba(74,144,226,0.75), 22, 0.35, 0, 0);";
            btn.setStyle(time.equals(current) ? selectedStyle : baseStyle);
            btn.setOnAction(e -> {
                if (currentMode == TimeMode.START) {
                    selectedStartTime = time;
                    updateStartDisplay();
                    DailySchedule schedule = facility.getWeeklyCalendar()
                            .getScheduleForDay(selectedDate.getDayOfWeek());
                    if (schedule != null) {
                        updateEndTimeOptions(selectedStartTime, schedule.getCloseTime());
                        updateEndDisplay();
                    }
                } else {
                    selectedEndTime = time;
                    updateEndDisplay();
                }
                hideTimeOverlay();
            });
            overlayTimesBox.getChildren().add(btn);
        }
    }

    private void showTimeOverlay() {
        timeOverlay.setVisible(true);
        timeOverlay.setManaged(true);
        mainLayout.setEffect(new GaussianBlur(5));
    }

    @FXML
    public void hideTimeOverlay() {
        timeOverlay.setVisible(false);
        timeOverlay.setManaged(false);
        if (mainLayout != null)
            mainLayout.setEffect(null);
    }

    private void updateStartDisplay() {
        startTimeDisplay.setText(selectedStartTime != null ? selectedStartTime.format(timeFormatter) : "Sec");
    }

    private void updateEndDisplay() {
        endTimeDisplay.setText(selectedEndTime != null ? selectedEndTime.format(timeFormatter) : "Sec");
    }

    @FXML
    public void handleNext() {
        if (selectedDate == null) {
            showError("Lutfen bir gun secin.");
            return;
        }
        if (selectedStartTime == null || selectedEndTime == null) {
            showError("Lutfen gecerli bir saat araligi secin.");
            return;
        }
        if (selectedDate.equals(LocalDate.now()) && selectedStartTime.isBefore(LocalTime.now())) {
            showError("Gecmise yonelik zaman secilemez.");
            return;
        }

        // Süre kontrolü: 1-3 saat arası olmalı
        long durationMinutes = java.time.Duration.between(selectedStartTime, selectedEndTime).toMinutes();
        if (durationMinutes < 60) {
            showError("Minimum rezervasyon suresi 1 saat.");
            return;
        }
        if (durationMinutes > 180) {
            showError("Maksimum rezervasyon suresi 3 saat.");
            return;
        }

        globalParams.setSelectedDate(selectedDate);
        globalParams.setSelectedStartTime(selectedStartTime);
        globalParams.setSelectedEndTime(selectedEndTime);
        userHomeController.setView("/fxml/user/reservation/user-reservation-step2.fxml");
    }

    @FXML
    public void handleBack() {
        userHomeController.setView("/fxml/user/facility/user-facility-dashboard.fxml");
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}