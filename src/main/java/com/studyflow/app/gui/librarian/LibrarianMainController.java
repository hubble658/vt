package com.studyflow.app.gui.librarian;

import com.studyflow.app.gui.NavigationService;
import com.studyflow.app.gui.ViewFactory;
import com.studyflow.app.model.user.User;
import com.studyflow.app.context.UserSessionContext;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.StackPane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class LibrarianMainController {

    @FXML
    private Label welcomeLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private StackPane contentArea;

    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnFacility;

    @Autowired
    private NavigationService navigationService;
    @Autowired
    private ViewFactory viewFactory;
    @Autowired
    private UserSessionContext userSessionContext;

    @FXML
    public void initialize() {
        dateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        updateWelcomeMessage(userSessionContext.getCurrentUser());
        showDashboard();
    }

    private void updateWelcomeMessage(User user) {
        welcomeLabel.setText((user != null) ? "Hosgeldiniz, Kutuphaneci " + user.getFirstName() : "Hosgeldiniz");
    }

    @FXML
    public void showDashboard() {
        resetButtonStyles();
        btnDashboard.getStyleClass().add("active");
        loadView("/fxml/librarian/librarian-home.fxml");
    }

    // --- FACILITY MENÜSÜ ---
    @FXML
    public void showFacilityMenu() {
        resetButtonStyles();
        btnFacility.getStyleClass().add("active");

        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("pop-menu");

        MenuItem itemUpdateSchedule = new MenuItem("Calisma Saatlerini Guncelle");
        MenuItem itemViewInfo = new MenuItem("Tesis Detaylarini Goruntule");
        MenuItem itemEditMap = new MenuItem("Tesis Haritasini Duzenle (Bloklar)");
        MenuItem itemEditDesks = new MenuItem("Masa Duzenini Duzenle");
        MenuItem itemEditSeats = new MenuItem("Koltuk Duzenini Duzenle");

        // Navigasyonlar
        itemUpdateSchedule.setOnAction(e -> loadView("/fxml/librarian/librarian-schedule.fxml"));
        itemViewInfo.setOnAction(e -> loadView("/fxml/librarian/editor/librarian-facility-details.fxml"));
        itemEditMap.setOnAction(e -> loadView("/fxml/librarian/editor/librarian-block-editor.fxml"));
        itemEditDesks.setOnAction(e -> loadView("/fxml/librarian/editor/librarian-desk-editor.fxml"));

        // Yeni Seat Editor Yönlendirmesi
        itemEditSeats.setOnAction(e -> loadView("/fxml/librarian/editor/librarian-seat-editor.fxml"));

        menu.getItems().addAll(itemUpdateSchedule, itemViewInfo, itemEditMap, itemEditDesks, itemEditSeats);
        menu.show(btnFacility, Side.RIGHT, 0, 0);
    }

    @FXML
    public void handleLogout() {
        userSessionContext.logout();
        navigationService.navigateTo(viewFactory.loadView("/fxml/auth/login.fxml"));
    }

    private void loadView(String fxmlPath) {
        Parent view = viewFactory.loadView(fxmlPath);
        contentArea.getChildren().setAll(view);
    }

    private void resetButtonStyles() {
        btnDashboard.getStyleClass().remove("active");
        btnFacility.getStyleClass().remove("active");
    }
}