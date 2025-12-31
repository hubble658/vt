package com.studyflow.app.gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ViewFactory {

    @Autowired
    private  ApplicationContext context;

    public Parent loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(context::getBean);
            return loader.load();
        } catch (IOException e) {
            throw new RuntimeException("FXML failed to load: " + fxmlPath, e);
        }
    }
}