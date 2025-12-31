package com.studyflow.app.gui.librarian;

import com.studyflow.app.context.UserSessionContext;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class LibrarianHomeController {

    // Dashboard Labels
    @FXML
    private Label statusLabel;

    // Dashboard Labels
    @FXML
    private Label facilityNameLabel;
    @FXML
    private Label currentDateLabel;
    @FXML
    private Label occupancyPercentLabel;
    @FXML
    private ProgressBar occupancyProgressBar;
    @FXML
    private Label occupiedSeatsLabel;
    @FXML
    private Label availableSeatsLabel;
    @FXML
    private Label totalSeatsInfoLabel;
    @FXML
    private Label totalBlocksLabel;
    @FXML
    private Label totalDesksLabel;
    @FXML
    private Label todayReservationsLabel;
    @FXML
    private Label weekReservationsLabel;

    // Bar Chart
    @FXML
    private BarChart<String, Number> weeklyBarChart;

    @Autowired
    private UserSessionContext userSessionContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @FXML
    public void initialize() {
        // İstatistikleri yükle
        loadStatistics();
    }

    private void loadStatistics() {
        try {
            Long facilityId = userSessionContext.getAssignedFacilityId();
            if (facilityId == null) {
                facilityNameLabel.setText("Tesis: Atanmadi");
                return;
            }

            // Tesis adını al
            String facilityName = jdbcTemplate.queryForObject(
                    "SELECT name FROM facilities WHERE id = ?", String.class, facilityId);
            facilityNameLabel.setText("Tesis: " + facilityName);

            // Bugünün tarihini göster
            currentDateLabel
                    .setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            // Tesis istatistikleri
            Integer blocks = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM facility_blocks WHERE facility_id = ?", Integer.class, facilityId);
            Integer desks = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM desks d JOIN facility_blocks fb ON d.facility_block_id = fb.id WHERE fb.facility_id = ?",
                    Integer.class, facilityId);
            Integer totalSeats = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM seats s JOIN desks d ON s.desk_id = d.id " +
                            "JOIN facility_blocks fb ON d.facility_block_id = fb.id WHERE fb.facility_id = ?",
                    Integer.class, facilityId);

            // Bugünkü rezervasyonlar
            Integer todayRes = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM reservations WHERE facility_id = ? AND reservation_date = ? AND status = 'ACTIVE'",
                    Integer.class, facilityId, LocalDate.now());

            // Bu haftaki rezervasyonlar
            LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
            LocalDate weekEnd = weekStart.plusDays(6);
            Integer weekRes = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM reservations WHERE facility_id = ? AND reservation_date BETWEEN ? AND ? AND status = 'ACTIVE'",
                    Integer.class, facilityId, weekStart, weekEnd);

            // Doluluk oranı hesapla
            int occupied = todayRes != null ? todayRes : 0;
            int total = totalSeats != null ? totalSeats : 0;
            double occupancyRate = total > 0 ? (occupied * 100.0 / total) : 0;

            // UI güncelle
            totalBlocksLabel.setText(String.valueOf(blocks != null ? blocks : 0));
            totalDesksLabel.setText(String.valueOf(desks != null ? desks : 0));
            todayReservationsLabel.setText(String.valueOf(todayRes != null ? todayRes : 0));
            weekReservationsLabel.setText(String.valueOf(weekRes != null ? weekRes : 0));

            // Doluluk gösterimi
            occupancyPercentLabel.setText(String.format("%.1f%%", occupancyRate));
            occupancyProgressBar.setProgress(occupancyRate / 100.0);
            occupiedSeatsLabel.setText("🟢 Dolu: " + occupied);
            availableSeatsLabel.setText("⚪ Musait: " + (total - occupied));
            totalSeatsInfoLabel.setText("📊 Toplam: " + total + " koltuk");

            // Progress bar rengini ayarla
            if (occupancyRate > 80) {
                occupancyProgressBar.setStyle("-fx-accent: #e74c3c;");
            } else if (occupancyRate > 50) {
                occupancyProgressBar.setStyle("-fx-accent: #f39c12;");
            } else {
                occupancyProgressBar.setStyle("-fx-accent: #27ae60;");
            }

            // Haftalık grafik verilerini yükle
            loadWeeklyChart(facilityId);

        } catch (Exception e) {
            System.err.println("İstatistik yükleme hatası: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadWeeklyChart(Long facilityId) {
        try {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Rezervasyonlar");

            LocalDate today = LocalDate.now();
            String[] dayNames = { "Pzt", "Sal", "Car", "Per", "Cum", "Cmt", "Paz" };

            for (int i = 0; i < 7; i++) {
                LocalDate date = today.with(DayOfWeek.MONDAY).plusDays(i);
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM reservations WHERE facility_id = ? AND reservation_date = ? AND status IN ('ACTIVE', 'COMPLETED')",
                        Integer.class, facilityId, date);

                String dayLabel = dayNames[i];
                if (date.equals(today)) {
                    dayLabel += " (Bugun)";
                }

                series.getData().add(new XYChart.Data<>(dayLabel, count != null ? count : 0));
            }

            weeklyBarChart.getData().clear();
            weeklyBarChart.getData().add(series);
            weeklyBarChart.setLegendVisible(false);

            // Bar renklerini ayarla
            for (XYChart.Data<String, Number> data : series.getData()) {
                if (data.getXValue().contains("Bugun")) {
                    data.getNode().setStyle("-fx-bar-fill: #4a90e2;");
                } else {
                    data.getNode().setStyle("-fx-bar-fill: #95a5a6;");
                }
            }

        } catch (Exception e) {
            System.err.println("Grafik yükleme hatası: " + e.getMessage());
        }
    }
}