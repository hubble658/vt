package com.studyflow.app.gui.user;

import com.studyflow.app.context.UserSessionContext;
import com.studyflow.app.gui.NavigationService;
import com.studyflow.app.gui.ViewFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserDashboardController {

    @FXML private Button btnExplore;
    @FXML private Button btnReservations;
    @FXML private Button btnProfile;

    @Autowired private UserHomeController userHomeController;
    @Autowired private UserSessionContext userSessionContext;
    @Autowired private NavigationService navigationService;
    @Autowired private ViewFactory viewFactory;

    @FXML
    public void initialize() {
        // Butonlara İkon Ekleme (Emoji veya FontIcon kullanılabilir)
        // Burada büyük fontlu Label kullanıyoruz.

        setupButtonIcon(btnExplore, "🏢", "icon-blue");
        setupButtonIcon(btnReservations, "📅", "icon-green");
        setupButtonIcon(btnProfile, "👤", "icon-purple");
    }

    private void setupButtonIcon(Button btn, String iconText, String cssClass) {
        Label icon = new Label(iconText);
        icon.getStyleClass().add(cssClass); // styles.css'deki renk/boyut
        btn.setGraphic(icon);
    }

    @FXML
    public void handleExploreFacilities() {
        userHomeController.showExploreFacilities();
    }

    @FXML
    public void handleMyReservations() {
        // YENİ: Rezervasyonlarım Sayfasına Git
        userHomeController.setView("/fxml/user/reservation/user-reservations.fxml");
    }

    @FXML
    public void handleProfile() {
        userHomeController.setView("/fxml/user/user-profile.fxml");
    }


    @FXML
    public void handleLogout() {
        userSessionContext.logout();
        navigationService.navigateTo(viewFactory.loadView("/fxml/auth/login.fxml"));
    }
}