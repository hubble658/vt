package com.studyflow.app.gui;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

@Component
@Setter
public class NavigationService {
    private BorderPane mainLayout;
    private final Stack<Node> history = new Stack<>();

    private final Map<String, Node> viewCache = new HashMap<>();

    public void navigateTo(Node viewNode) {
        if (mainLayout.getCenter() != null) {
            history.push(mainLayout.getCenter());
        }

        mainLayout.setCenter(viewNode);
    }

    public void jumpTo(Node viewNode){
        history.clear();
        mainLayout.setCenter(viewNode);
    }

    public void goBack() {
        if (!history.isEmpty()) {
            Node previousView = history.pop();
            mainLayout.setCenter(previousView);
        }
    }

    public void clearHistory() {
        history.clear();
    }
}