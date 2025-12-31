package com.studyflow.app.gui.admin;

import com.studyflow.app.context.UserSessionContext;
import com.studyflow.app.gui.NavigationService;
import com.studyflow.app.gui.ViewFactory;
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
public class AdminMainController {

    @FXML private Label welcomeLabel;
    @FXML private Label dateLabel;
    @FXML private StackPane contentArea;

    @FXML private Button btnDashboard;
    @FXML private Button btnFacilities;
    @FXML private Button btnLibrarians;

    @Autowired
    private NavigationService navigationService;

    @Autowired
    private ViewFactory viewFactory;

    @Autowired
    private UserSessionContext userSessionContext;

    @FXML
    public void initialize() {
        dateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        if (userSessionContext.getCurrentUser() != null) {
            welcomeLabel.setText("Welcome, " + userSessionContext.getCurrentUser().getFirstName());
        }
        showDashboard();
    }

    @FXML
    public void showDashboard() {
        resetButtonStyles();
        btnDashboard.getStyleClass().add("active");
        // Facilities Dashboard - Admin ana sayfasi
        loadView("/fxml/admin/admin-home.fxml");
    }

    @FXML
    public void showFacilitiesMenu() {
        resetButtonStyles();
        btnFacilities.getStyleClass().add("active");

        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("pop-menu");

        MenuItem itemList = new MenuItem("List All Facilities");
        MenuItem itemCreate = new MenuItem("Create New Facility");

        itemList.setOnAction(e -> loadView("/fxml/admin/admin-facilities-list.fxml"));
        itemCreate.setOnAction(e -> loadView("/fxml/admin/admin-create-facility.fxml"));

        menu.getItems().addAll(itemList, itemCreate);
        menu.show(btnFacilities, Side.RIGHT, 0, 0);
    }

    @FXML
    public void showLibrariansMenu() {
        resetButtonStyles();
        btnLibrarians.getStyleClass().add("active");

        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("pop-menu");

        MenuItem itemPromote = new MenuItem("Promote User to Librarian");

        itemPromote.setOnAction(e -> loadView("/fxml/admin/admin-promote-librarian.fxml"));

        menu.getItems().addAll(itemPromote);
        menu.show(btnLibrarians, Side.RIGHT, 0, 0);
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
    
    /**
     * Diger controller'lardan view yukleme icin public metod
     */
    public void loadViewFromExternal(String fxmlPath) {
        loadView(fxmlPath);
    }

    private void resetButtonStyles() {
        btnDashboard.getStyleClass().remove("active");
        btnFacilities.getStyleClass().remove("active");
        btnLibrarians.getStyleClass().remove("active");
    }
}