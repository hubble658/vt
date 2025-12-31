package com.studyflow.app.gui.user;

import com.studyflow.app.context.UserSessionContext;
import com.studyflow.app.model.reservation.Reservation;
import com.studyflow.app.model.user.User;
import com.studyflow.app.model.dto.UserProfileStats;
import com.studyflow.app.service.user.UserProfileService;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;

@Component
public class UserProfileController {

    @FXML
    private Label avatarLabel;
    @FXML
    private Label userNameLabel;
    @FXML
    private Label userEmailLabel;
    @FXML
    private Label userRoleLabel;

    @FXML
    private Label lblTotalTime;
    @FXML
    private Label lblTrendArrow;
    @FXML
    private Label lblTrendText;
    @FXML
    private Label lblProductiveTime;
    @FXML
    private Label lblProductiveMsg;

    @FXML
    private Label lblAvgSession;
    @FXML
    private Label lblLongestSession;
    @FXML
    private Label lblShortestSession;

    @FXML
    private BarChart<String, Number> trendChart;
    @FXML
    private Label lblTrendNoData; // YENİ

    @FXML
    private BarChart<String, Number> longTermChart;
    @FXML
    private Label lblLongTermNoData; // YENİ
    @FXML
    private ComboBox<String> periodSelector;
    @FXML
    private ComboBox<String> monthSelector;

    @Autowired
    private UserProfileService userProfileService;
    @Autowired
    private UserSessionContext userSessionContext;
    @Autowired
    private UserHomeController userHomeController;

    private List<Reservation> historyData;

    @FXML
    public void initialize() {
        loadUserInfo();
        this.historyData = userProfileService.getAllHistory();
        loadStatistics();
        setupLongTermChart();
    }

    private void loadUserInfo() {
        User user = userSessionContext.getCurrentUser();
        if (user != null) {
            userNameLabel.setText(user.getFirstName() + " " + user.getLastName());
            userEmailLabel.setText(user.getEmail());
            userRoleLabel.setText(user.getUserRole().name());
            if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
                avatarLabel.setText(user.getFirstName().substring(0, 1).toUpperCase());
            }
        }
    }

    private void loadStatistics() {
        UserProfileStats stats = userProfileService.calculateUserStats();
        if (stats == null)
            return;

        lblTotalTime.setText(stats.getTotalTimeThisWeek());
        lblTrendText.setText(stats.getWeeklyComparison());

        if (stats.isIncrease()) {
            lblTrendArrow.setText("↑");
            lblTrendArrow.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 20px; -fx-font-weight: bold;");
        } else {
            lblTrendArrow.setText("↓");
            lblTrendArrow.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 20px; -fx-font-weight: bold;");
        }

        lblProductiveTime.setText(stats.getMostProductiveRange());
        lblProductiveMsg.setText(stats.getProductivityMessage());
        lblAvgSession.setText(stats.getAverageDuration());
        lblLongestSession.setText(stats.getLongestSession());
        lblShortestSession.setText(stats.getShortestSession());

        // Trend Grafiği için No Data etiketiyle birlikte çağır
        populateChart(trendChart, stats.getLast4WeeksTrend(), lblTrendNoData);
    }

    private void setupLongTermChart() {
        periodSelector.getItems().addAll("Son 3 Ay", "Son 12 Ay", "Belirli Bir Ay...");
        periodSelector.setValue("Son 3 Ay");

        for (Month m : Month.values()) {
            monthSelector.getItems().add(getTurkishMonthName(m));
        }
        monthSelector.setValue(getTurkishMonthName(LocalDate.now().getMonth()));
        monthSelector.setVisible(false);
        monthSelector.setManaged(false);

        periodSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if ("Belirli Bir Ay...".equals(newVal)) {
                monthSelector.setVisible(true);
                monthSelector.setManaged(true);
                updateSpecificMonthChart();
            } else {
                monthSelector.setVisible(false);
                monthSelector.setManaged(false);
                updatePeriodChart(newVal);
            }
        });

        monthSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (periodSelector.getValue().equals("Belirli Bir Ay...")) {
                updateSpecificMonthChart();
            }
        });

        updatePeriodChart("Son 3 Ay");
    }

    private void updatePeriodChart(String period) {
        int months = period.equals("Son 12 Ay") ? 12 : 3;
        Map<String, Double> data = userProfileService.calculateMonthlyTrend(historyData, months);
        populateChart(longTermChart, data, lblLongTermNoData);
    }

    private void updateSpecificMonthChart() {
        String selectedMonthName = monthSelector.getValue();
        if (selectedMonthName == null)
            return;
        // Türkçe ay ismini tekrar Enum'a çevirmek için ters işlem gerekebilir veya
        // index kullanılabilir.
        // Basitlik için UI'dan gelen stringi parse etmek yerine index kullanalim ya da
        // switch.
        // Ancak burada combobox string tutuyor.
        Month month = getMonthFromTurkishName(selectedMonthName);
        if (month == null)
            return;
        Map<String, Double> data = userProfileService.calculateSpecificMonthTrend(historyData, month);
        populateChart(longTermChart, data, lblLongTermNoData);
    }

    private Month getMonthFromTurkishName(String name) {
        if (name == null)
            return null;
        switch (name) {
            case "Ocak":
                return Month.JANUARY;
            case "Subat":
                return Month.FEBRUARY;
            case "Mart":
                return Month.MARCH;
            case "Nisan":
                return Month.APRIL;
            case "Mayis":
                return Month.MAY;
            case "Haziran":
                return Month.JUNE;
            case "Temmuz":
                return Month.JULY;
            case "Agustos":
                return Month.AUGUST;
            case "Eylul":
                return Month.SEPTEMBER;
            case "Ekim":
                return Month.OCTOBER;
            case "Kasim":
                return Month.NOVEMBER;
            case "Aralik":
                return Month.DECEMBER;
            default:
                return null;
        }
    }

    // GÜNCELLENMİŞ POPULATE METODU (NO DATA LOGIC)
    private void populateChart(BarChart<String, Number> chart, Map<String, Double> dataMap, Label noDataLabel) {
        chart.setAnimated(false);
        chart.getData().clear();
        chart.layout();

        // Toplam değeri hesapla (Veri var mı kontrolü için)
        double totalValue = dataMap.values().stream().mapToDouble(Double::doubleValue).sum();

        if (totalValue <= 0) {
            // VERİ YOK: Grafiği gizle, yazıyı göster
            chart.setVisible(false);
            noDataLabel.setVisible(true);
        } else {
            // VERİ VAR: Grafiği göster, yazıyı gizle
            chart.setVisible(true);
            noDataLabel.setVisible(false);

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("");

            for (Map.Entry<String, Double> entry : dataMap.entrySet()) {
                XYChart.Data<String, Number> data = new XYChart.Data<>(entry.getKey(), entry.getValue());
                data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        displayLabelForData(data);
                    }
                });
                series.getData().add(data);
            }
            chart.getData().add(series);
        }
    }

    private void displayLabelForData(XYChart.Data<String, Number> data) {
        final StackPane bar = (StackPane) data.getNode();
        if (data.getYValue().doubleValue() <= 0)
            return;
        Label dataLabel = new Label(data.getYValue() + "h");
        dataLabel.setStyle(
                "-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white; -fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.3), 2, 0, 0, 1);");
        bar.getChildren().add(dataLabel);
    }

    @FXML
    public void handleBack() {
        userHomeController.showDashboard();
    }

    private String getTurkishMonthName(Month month) {
        switch (month) {
            case JANUARY:
                return "Ocak";
            case FEBRUARY:
                return "Subat";
            case MARCH:
                return "Mart";
            case APRIL:
                return "Nisan";
            case MAY:
                return "Mayis";
            case JUNE:
                return "Haziran";
            case JULY:
                return "Temmuz";
            case AUGUST:
                return "Agustos";
            case SEPTEMBER:
                return "Eylul";
            case OCTOBER:
                return "Ekim";
            case NOVEMBER:
                return "Kasim";
            case DECEMBER:
                return "Aralik";
            default:
                return month.name();
        }
    }
}