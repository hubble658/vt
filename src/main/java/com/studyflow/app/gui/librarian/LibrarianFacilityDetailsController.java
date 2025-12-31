package com.studyflow.app.gui.librarian;

import com.studyflow.app.model.facility.DailySchedule;
import com.studyflow.app.model.facility.Facility;
import com.studyflow.app.model.user.User;
import com.studyflow.app.repository.facility.FacilityRepository;
import com.studyflow.app.context.UserSessionContext;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class LibrarianFacilityDetailsController {

    @FXML private ImageView facilityImage;
    @FXML private Label nameLabel;
    @FXML private Label addressLabel;
    @FXML private VBox scheduleContainer;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private UserSessionContext userSessionContext;

    @FXML
    public void initialize() {
        loadFacilityDetails();
    }

    private void loadFacilityDetails() {
        User currentUser = userSessionContext.getCurrentUser();

        // Güvenlik: Kullanıcının tesisi var mı?
        if (currentUser == null || currentUser.getFacilityInfo() == null) {
            nameLabel.setText("No Facility Assigned");
            addressLabel.setText("Please contact admin.");
            return;
        }

        Facility facility = currentUser.getFacilityInfo().getFacility();


        if (facility != null) {
            populateUI(facility);
        } else {
            nameLabel.setText("Facility Not Found");
        }
    }

    private void populateUI(Facility facility) {
        // 1. Metin Bilgileri
        nameLabel.setText(facility.getName());
        addressLabel.setText(facility.getAddress());

        // 2. Resim Yükleme (/resources/facility/resimAdi.jpg)
        try {
            String imagePath = "/facility/" + facility.getImageUrl();
            // Dosyanın varlığını kontrol etmek için stream açıyoruz
            if (getClass().getResource(imagePath) != null) {
                Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
                facilityImage.setImage(image);
            } else {
                System.err.println("Resim bulunamadı: " + imagePath);
                // İstersen buraya default bir resim yükleyebilirsin
            }
        } catch (Exception e) {
            System.err.println("Resim yüklenirken hata: " + e.getMessage());
        }

        // 3. Çalışma Saatleri (Schedule)
        renderSchedule(facility);
    }

    private void renderSchedule(Facility facility) {
        scheduleContainer.getChildren().clear();

        if (facility.getWeeklyCalendar() == null || facility.getWeeklyCalendar().getDays().isEmpty()) {
            scheduleContainer.getChildren().add(new Label("No schedule defined yet."));
            return;
        }

        // Günleri Sırala (MONDAY -> SUNDAY)
        List<DailySchedule> sortedDays = facility.getWeeklyCalendar().getDays();
        sortedDays.sort(Comparator.comparing(DailySchedule::getDayOfWeek));

        for (DailySchedule day : sortedDays) {
            HBox row = createScheduleRow(day);
            scheduleContainer.getChildren().add(row);
        }
    }

    // Her bir gün için şık bir satır oluşturur
    private HBox createScheduleRow(DailySchedule day) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(20);
        row.setStyle("-fx-border-color: #eee; -fx-border-width: 0 0 1 0; -fx-padding: 10 0;");

        // Gün Adı (Pazartesi, Salı...)
        String dayName = day.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        Label dayLabel = new Label(dayName);
        dayLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #555; -fx-font-size: 14px;");
        dayLabel.setPrefWidth(120); // Sabit genişlik hizalama için

        // Saat Bilgisi
        Label timeLabel = new Label();
        timeLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px;");

        if (day.getIsClosed()) {
            timeLabel.setText("CLOSED");
            timeLabel.setStyle(timeLabel.getStyle() + "-fx-text-fill: #e74c3c; -fx-font-weight: bold;"); // Kırmızı
        } else {
            String open = day.getOpenTime() != null ? day.getOpenTime().toString() : "--:--";
            String close = day.getCloseTime() != null ? day.getCloseTime().toString() : "--:--";
            timeLabel.setText(open + " - " + close);
            timeLabel.setStyle(timeLabel.getStyle() + "-fx-text-fill: #27ae60;"); // Yeşil
        }

        row.getChildren().addAll(dayLabel, timeLabel);
        return row;
    }
}