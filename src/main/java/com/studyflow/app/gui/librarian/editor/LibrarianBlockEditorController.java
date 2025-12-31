package com.studyflow.app.gui.librarian.editor;

import com.studyflow.app.gui.NavigationService;
import com.studyflow.app.gui.ViewFactory;
import com.studyflow.app.model.facility.Facility;
import com.studyflow.app.model.facility.FacilityBlock;
import com.studyflow.app.repository.facility.FacilityBlockRepository;
import com.studyflow.app.context.UserSessionContext;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class LibrarianBlockEditorController {

    private static final double MAP_LIMIT = 600.0;

    @FXML private Pane mapCanvas;
    @FXML private VBox propertiesPanel;
    @FXML private TextField blockNameField;
    @FXML private Slider widthSlider;
    @FXML private Slider heightSlider;
    @FXML private ColorPicker colorPicker;
    @FXML private Label statusLabel;
    @FXML private Button btnUndo;

    @Autowired
    private FacilityBlockRepository blockRepository;

    @Autowired
    private UserSessionContext userSessionContext;

    @Autowired
    private NavigationService navigationService;

    @Autowired
    private ViewFactory viewFactory;

    private final Map<StackPane, FacilityBlock> uiBlockMap = new HashMap<>();
    private final List<FacilityBlock> blocksToDelete = new ArrayList<>();
    private final Stack<HistoryAction> undoStack = new Stack<>();

    private StackPane selectedNode;
    private double startX, startY;
    private double tempOldX, tempOldY, tempOldWidth, tempOldHeight;

    private enum ActionType { ADD, DELETE, MODIFY }

    private static class HistoryAction {
        ActionType type;
        StackPane uiNode;
        FacilityBlock data;
        double prevX, prevY, prevWidth, prevHeight;

        public HistoryAction(ActionType type, StackPane uiNode, FacilityBlock data) {
            this.type = type;
            this.uiNode = uiNode;
            this.data = data;
        }

        public HistoryAction(ActionType type, StackPane uiNode, FacilityBlock data, double x, double y, double w, double h) {
            this.type = type;
            this.uiNode = uiNode;
            this.data = data;
            this.prevX = x;
            this.prevY = y;
            this.prevWidth = w;
            this.prevHeight = h;
        }
    }

    @FXML
    public void initialize() {
        statusLabel.managedProperty().bind(statusLabel.visibleProperty());
        mapCanvas.setPrefSize(MAP_LIMIT, MAP_LIMIT);
        mapCanvas.setMinSize(MAP_LIMIT, MAP_LIMIT);
        mapCanvas.setMaxSize(MAP_LIMIT, MAP_LIMIT);

        loadBlocksFromDatabase();
        updateUndoButtonState();

        // Listeners
        setupSliderUndoLogic(widthSlider, "WIDTH");
        widthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedNode != null) {
                double newWidth = newVal.doubleValue();
                double currentX = selectedNode.getLayoutX();
                if (currentX + newWidth > MAP_LIMIT) newWidth = MAP_LIMIT - currentX;

                Rectangle rect = (Rectangle) selectedNode.getChildren().get(0);
                rect.setWidth(newWidth);
                selectedNode.setPrefWidth(newWidth);
            }
        });

        setupSliderUndoLogic(heightSlider, "HEIGHT");
        heightSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedNode != null) {
                double newHeight = newVal.doubleValue();
                double currentY = selectedNode.getLayoutY();
                if (currentY + newHeight > MAP_LIMIT) newHeight = MAP_LIMIT - currentY;

                Rectangle rect = (Rectangle) selectedNode.getChildren().get(0);
                rect.setHeight(newHeight);
                selectedNode.setPrefHeight(newHeight);
            }
        });

        colorPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedNode != null) {
                Rectangle rect = (Rectangle) selectedNode.getChildren().get(0);
                rect.setFill(newVal);
                rect.setOpacity(0.7);
            }
        });

        blockNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if(selectedNode != null){
                Label label = (Label) selectedNode.getChildren().get(1);
                label.setText(newVal);
            }
        });
    }

    private void setupSliderUndoLogic(Slider slider, String type) {
        slider.setOnMousePressed(e -> {
            if (selectedNode != null) captureSnapshot(selectedNode);
        });
        slider.setOnMouseReleased(e -> {
            if (selectedNode != null) {
                Rectangle rect = (Rectangle) selectedNode.getChildren().get(0);
                boolean changed = false;
                if (type.equals("WIDTH") && rect.getWidth() != tempOldWidth) changed = true;
                if (type.equals("HEIGHT") && rect.getHeight() != tempOldHeight) changed = true;
                if (changed) pushModifyAction(selectedNode);
            }
        });
    }

    private void loadBlocksFromDatabase() {
        uiBlockMap.clear();
        blocksToDelete.clear();
        undoStack.clear();
        mapCanvas.getChildren().clear();
        updateUndoButtonState();
        propertiesPanel.setDisable(true);
        selectedNode = null;

        Facility facility = userSessionContext.getCurrentUser().getFacilityInfo().getFacility();
        if (facility == null) return;

        // DÜZELTME: JPA findByFacilityId yerine Native Query findAllByFacilityId çağırıyoruz
        for (FacilityBlock block : blockRepository.findAllByFacilityId(facility.getId())) {
            createBlockOnCanvas(block);
        }
    }

    @FXML
    public void handleAddBlock() {
        FacilityBlock newBlock = FacilityBlock.builder()
                .name("New Block")
                .x(50).y(50).width(100).height(80)
                .colorHex("#4a90e2")
                .currentIdIndex(0) // Default index
                .build();

        StackPane newNode = createBlockOnCanvas(newBlock);
        undoStack.push(new HistoryAction(ActionType.ADD, newNode, newBlock));
        updateUndoButtonState();
    }

    private StackPane createBlockOnCanvas(FacilityBlock blockData) {
        StackPane blockNode = new StackPane();
        blockNode.setLayoutX(blockData.getX());
        blockNode.setLayoutY(blockData.getY());
        blockNode.setPrefSize(blockData.getWidth(), blockData.getHeight());
        blockNode.setCursor(Cursor.HAND);

        Rectangle rect = new Rectangle(blockData.getWidth(), blockData.getHeight());
        try {
            rect.setFill(Color.web(blockData.getColorHex() != null ? blockData.getColorHex() : "#4a90e2"));
        } catch (Exception e) { rect.setFill(Color.DODGERBLUE); }

        rect.setOpacity(0.7);
        rect.setStroke(Color.BLACK);
        rect.setStrokeWidth(1);
        rect.setStrokeType(StrokeType.INSIDE);

        Label label = new Label(blockData.getName());
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-effect: dropshadow(one-pass-box, black, 2, 0.5, 0, 0);");

        blockNode.getChildren().addAll(rect, label);

        mapCanvas.getChildren().add(blockNode);
        uiBlockMap.put(blockNode, blockData);

        addInteractionLogic(blockNode);
        return blockNode;
    }

    private void addInteractionLogic(StackPane node) {
        node.setOnMouseClicked(event -> {
            selectBlock(node);
            event.consume();
        });

        node.setOnMousePressed(event -> {
            selectBlock(node);
            startX = event.getSceneX() - node.getLayoutX();
            startY = event.getSceneY() - node.getLayoutY();
            node.setCursor(Cursor.CLOSED_HAND);
            captureSnapshot(node);
        });

        node.setOnMouseDragged(event -> {
            double newX = event.getSceneX() - startX;
            double newY = event.getSceneY() - startY;
            newX = clamp(newX, 0, MAP_LIMIT - node.getWidth());
            newY = clamp(newY, 0, MAP_LIMIT - node.getHeight());
            node.setLayoutX(newX);
            node.setLayoutY(newY);
        });

        node.setOnMouseReleased(event -> {
            node.setCursor(Cursor.HAND);
            if (node.getLayoutX() != tempOldX || node.getLayoutY() != tempOldY) {
                pushModifyAction(node);
            }
        });
    }

    private void captureSnapshot(StackPane node) {
        Rectangle rect = (Rectangle) node.getChildren().get(0);
        tempOldX = node.getLayoutX();
        tempOldY = node.getLayoutY();
        tempOldWidth = rect.getWidth();
        tempOldHeight = rect.getHeight();
    }

    private void pushModifyAction(StackPane node) {
        FacilityBlock data = uiBlockMap.get(node);
        undoStack.push(new HistoryAction(ActionType.MODIFY, node, data, tempOldX, tempOldY, tempOldWidth, tempOldHeight));
        updateUndoButtonState();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    private void selectBlock(StackPane node) {
        if (this.selectedNode != null && this.selectedNode != node) {
            updateBlockVisuals(this.selectedNode, false);
        }
        this.selectedNode = node;
        updateBlockVisuals(node, true);

        FacilityBlock data = uiBlockMap.get(node);
        Rectangle rect = (Rectangle) node.getChildren().get(0);

        propertiesPanel.setDisable(false);
        blockNameField.setText(data.getName());
        widthSlider.setValue(rect.getWidth());
        heightSlider.setValue(rect.getHeight());
        colorPicker.setValue((Color) rect.getFill());

        node.toFront();
    }

    private void updateBlockVisuals(StackPane node, boolean isSelected) {
        if (node == null) return;
        Rectangle rect = (Rectangle) node.getChildren().get(0);
        if (isSelected) {
            rect.setStroke(Color.RED);
            rect.setStrokeWidth(3);
            rect.setEffect(new DropShadow(10, Color.BLACK));
        } else {
            rect.setStroke(Color.BLACK);
            rect.setStrokeWidth(1);
            rect.setEffect(null);
        }
    }

    @FXML
    public void handleDeleteBlock() {
        if (selectedNode != null) {
            FacilityBlock data = uiBlockMap.get(selectedNode);
            if (data.getId() != null) {
                blocksToDelete.add(data);
            }
            undoStack.push(new HistoryAction(ActionType.DELETE, selectedNode, data));
            updateUndoButtonState();

            mapCanvas.getChildren().remove(selectedNode);
            uiBlockMap.remove(selectedNode);
            selectedNode = null;
            propertiesPanel.setDisable(true);
            showSuccess("Block removed (Click Save to confirm)");
        }
    }

    @FXML
    public void handleUndo() {
        if (undoStack.isEmpty()) return;
        HistoryAction lastAction = undoStack.pop();

        if (lastAction.type == ActionType.ADD) {
            mapCanvas.getChildren().remove(lastAction.uiNode);
            uiBlockMap.remove(lastAction.uiNode);
            selectedNode = null;
            propertiesPanel.setDisable(true);
            showSuccess("Undo: Addition cancelled.");
        } else if (lastAction.type == ActionType.DELETE) {
            mapCanvas.getChildren().add(lastAction.uiNode);
            uiBlockMap.put(lastAction.uiNode, lastAction.data);
            if (lastAction.data.getId() != null) blocksToDelete.remove(lastAction.data);
            addInteractionLogic(lastAction.uiNode);
            selectBlock(lastAction.uiNode);
            showSuccess("Undo: Block restored.");
        } else if (lastAction.type == ActionType.MODIFY) {
            StackPane node = lastAction.uiNode;
            Rectangle rect = (Rectangle) node.getChildren().get(0);
            node.setLayoutX(lastAction.prevX);
            node.setLayoutY(lastAction.prevY);
            node.setPrefSize(lastAction.prevWidth, lastAction.prevHeight);
            rect.setWidth(lastAction.prevWidth);
            rect.setHeight(lastAction.prevHeight);
            if (selectedNode == node) {
                widthSlider.setValue(lastAction.prevWidth);
                heightSlider.setValue(lastAction.prevHeight);
            }
            showSuccess("Undo: Move/Resize reverted.");
        }
        updateUndoButtonState();
    }

    /**
     * DÜZELTME: Kaydetme Mantığı (Native SQL)
     * Save/Delete yerine Insert/Update/Delete Query'leri çağırıyoruz.
     */
    @FXML
    public void handleSaveChanges() {
        try {
            Facility facility = userSessionContext.getCurrentUser().getFacilityInfo().getFacility();

            // 1. Silinecekler
            if (!blocksToDelete.isEmpty()) {
                for (FacilityBlock block : blocksToDelete) {
                    blockRepository.deleteBlockById(block.getId());
                }
                blocksToDelete.clear();
            }

            // 2. Ekleme ve Güncellemeler
            for (Map.Entry<StackPane, FacilityBlock> entry : uiBlockMap.entrySet()) {
                StackPane node = entry.getKey();
                FacilityBlock data = entry.getValue();
                Rectangle rect = (Rectangle) node.getChildren().get(0);
                Label label = (Label) node.getChildren().get(1);

                // UI'dan Güncel Verileri Al
                String name = label.getText();
                double x = node.getLayoutX();
                double y = node.getLayoutY();
                double w = rect.getWidth();
                double h = rect.getHeight();
                String color = toHexString((Color) rect.getFill());
                Integer currentIndex = (data.getCurrentIdIndex() == null) ? 0 : data.getCurrentIdIndex();

                if (data.getId() == null) {
                    // ID YOK -> INSERT
                    blockRepository.insertBlock(name, x, y, w, h, color, currentIndex, facility.getId());
                } else {
                    // ID VAR -> UPDATE
                    blockRepository.updateBlock(data.getId(), name, x, y, w, h, color);
                }
            }

            undoStack.clear();
            updateUndoButtonState();

            // ÖNEMLİ: Native insert ID döndürmediği için listeyi baştan yükle
            loadBlocksFromDatabase();

            showSuccess("All changes saved successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            showSuccess("Error: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    private void updateUndoButtonState() {
        if (btnUndo != null) {
            btnUndo.setDisable(undoStack.isEmpty());
        }
    }

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X", (int)(color.getRed()*255), (int)(color.getGreen()*255), (int)(color.getBlue()*255));
    }

    private void showSuccess(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: #27ae60;");
        statusLabel.setVisible(true);
    }
}