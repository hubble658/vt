package com.studyflow.app.gui;

import com.studyflow.app.Main;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.util.Objects;

public class StudyFlow extends Application {
    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() {
        applicationContext = new SpringApplicationBuilder(Main.class).run();
    }

    @Override
    public void stop() {
        applicationContext.close();
    }

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main_layout.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            Scene scene = new Scene(root, 1024, 768);
            // scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setTitle("StudyFlow");
            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icon/StudyFlow.png"))));
            stage.setScene(scene);
            stage.setResizable(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Main Screen Failed to load!", e);
        }
    }

    public static void main(String[] args) {
        launch();
    }

}