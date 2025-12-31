package com.studyflow.app.gui.librarian.editor;

import com.studyflow.app.gui.NavigationService;
import com.studyflow.app.gui.ViewFactory;
import com.studyflow.app.model.facility.Desk;
import com.studyflow.app.model.facility.Facility;
import com.studyflow.app.model.facility.FacilityBlock;
import com.studyflow.app.repository.facility.DeskRepository;
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
import javafx.scene.text.TextAlignment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class LibrarianDeskEditorController {

    private static final double MAP_LIMIT = 600.0;

    @FXML private ScrollPane facilityScroll;
    @FXML private ScrollPane deskScroll;
    @FXML private Pane facilityCanvas;
    @FXML private Pane deskCanvas;
    @FXML private VBox selectionPanel;
    @FXML private VBox editorPanel;
    @FXML private Button btnBackToMap;
    @FXML private Label breadcrumbLabel;
    @FXML private Label blockInfoLabel;
    @FXML private VBox propertiesBox;
    @FXML private Spinner<Integer> sizeSpinner;
    @FXML private Spinner<Integer> allocationSpinner;
    @FXML private Label rangePreviewLabel;
    @FXML private Slider widthSlider;
    @FXML private Slider heightSlider;
    @FXML private ColorPicker colorPicker;
    @FXML private Label statusLabel;
    @FXML private Button btnUndo;

    @Autowired private DeskRepository deskRepository;
    @Autowired private FacilityBlockRepository blockRepository;
    @Autowired private NavigationService navigationService;
    @Autowired private ViewFactory viewFactory;
    @Autowired private UserSessionContext userSessionContext;

    private FacilityBlock currentBlock;
    private Integer tempBlockIndexCounter;

    private final Map<StackPane, Desk> uiDeskMap = new HashMap<>();
    private final List<Desk> desksToDelete = new ArrayList<>();
    private final Stack<HistoryAction> undoStack = new Stack<>();

    private StackPane selectedNode;
    private double startX, startY;
    private double tempOldX, tempOldY, tempOldWidth, tempOldHeight;

    private enum ActionType { ADD, DELETE, MODIFY }
    private static class HistoryAction {
        ActionType type; StackPane uiNode; Desk data;
        double prevX, prevY, prevW, prevH;
        public HistoryAction(ActionType type, StackPane uiNode, Desk data) { this.type = type; this.uiNode = uiNode; this.data = data; }
        public HistoryAction(ActionType type, StackPane uiNode, Desk data, double x, double y, double w, double h) {
            this.type = type; this.uiNode = uiNode; this.data = data;
            this.prevX = x; this.prevY = y; this.prevW = w; this.prevH = h;
        }
    }

    @FXML
    public void initialize() {
        showFacilityMapMode();
        setupEditorListeners();
        deskCanvas.setPrefSize(MAP_LIMIT, MAP_LIMIT);
        deskCanvas.setMinSize(MAP_LIMIT, MAP_LIMIT);
        deskCanvas.setMaxSize(MAP_LIMIT, MAP_LIMIT);
    }

    private void showFacilityMapMode() {
        facilityScroll.setVisible(true);
        deskScroll.setVisible(false);
        selectionPanel.setVisible(true);
        editorPanel.setVisible(false);
        btnBackToMap.setVisible(false);
        breadcrumbLabel.setText(" > Select a Block");
        loadFacilityMap();
    }

    private void switchToDeskMode(FacilityBlock block) {
        this.currentBlock = block;
        this.tempBlockIndexCounter = block.getCurrentIdIndex() != null ? block.getCurrentIdIndex() : 0;

        facilityScroll.setVisible(false);
        deskScroll.setVisible(true);
        selectionPanel.setVisible(false);
        editorPanel.setVisible(true);
        btnBackToMap.setVisible(true);
        breadcrumbLabel.setText(" > Editing: " + block.getName());
        blockInfoLabel.setText("Editing: " + block.getName());
        loadDesksForBlock();
    }

    @FXML
    public void handleBackToMap() {
        showFacilityMapMode();
        uiDeskMap.clear();
        desksToDelete.clear();
        undoStack.clear();
        deskCanvas.getChildren().clear();
        propertiesBox.setDisable(true);
        selectedNode = null;
    }


    private void loadFacilityMap() {
        facilityCanvas.getChildren().clear();
        Facility facility = userSessionContext.getCurrentUser().getFacilityInfo().getFacility();
        if (facility == null) return;
        for (FacilityBlock block : blockRepository.findAllByFacilityId(facility.getId())) {
            StackPane node = createReadOnlyBlock(block);
            facilityCanvas.getChildren().add(node);
        }
    }

    private StackPane createReadOnlyBlock(FacilityBlock block) {
        StackPane node = new StackPane();
        node.setLayoutX(block.getX()); node.setLayoutY(block.getY());
        node.setPrefSize(block.getWidth(), block.getHeight());
        node.setCursor(Cursor.HAND);
        Rectangle rect = new Rectangle(block.getWidth(), block.getHeight());
        try { rect.setFill(Color.web(block.getColorHex())); } catch (Exception e) { rect.setFill(Color.GRAY); }
        rect.setStroke(Color.BLACK); rect.setOpacity(0.9);
        Label label = new Label(block.getName());
        label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-effect: dropshadow(one-pass-box, black, 2, 0.5, 0, 0);");
        node.getChildren().addAll(rect, label);
        node.setOnMouseClicked(e -> switchToDeskMode(block));
        node.setOnMouseEntered(e -> { rect.setStroke(Color.ORANGE); rect.setStrokeWidth(3); });
        node.setOnMouseExited(e -> { rect.setStroke(Color.BLACK); rect.setStrokeWidth(1); });
        return node;
    }

    private void loadDesksForBlock() {
        deskCanvas.getChildren().clear();
        uiDeskMap.clear();
        desksToDelete.clear();
        undoStack.clear();
        updateUndoButtonState();
        propertiesBox.setDisable(true);
        selectedNode = null;
        for (Desk desk : deskRepository.findAllByBlockId(currentBlock.getId())) {
            createDeskOnCanvas(desk);
        }
    }

    @FXML
    public void handleAddDesk() {
        Desk newDesk = Desk.builder().size(4).currentIdIndex(0).x(50).y(50).width(100).height(60).colorHex("#8e44ad").build();
        StackPane node = createDeskOnCanvas(newDesk);
        undoStack.push(new HistoryAction(ActionType.ADD, node, newDesk));
        updateUndoButtonState();
    }

    private StackPane createDeskOnCanvas(Desk deskData) {
        StackPane node = new StackPane();
        node.setLayoutX(deskData.getX()); node.setLayoutY(deskData.getY());
        node.setPrefSize(deskData.getWidth(), deskData.getHeight());
        node.setCursor(Cursor.HAND);

        Rectangle rect = new Rectangle(deskData.getWidth(), deskData.getHeight());
        try { rect.setFill(Color.web(deskData.getColorHex())); } catch (Exception e) { rect.setFill(Color.PURPLE); }
        rect.setStroke(Color.BLACK); rect.setOpacity(0.8);

        String labelText = (deskData.getIdRange() != null) ? deskData.getIdRange() : "New";
        Label label = new Label(labelText);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-effect: dropshadow(one-pass-box, black, 2, 0.5, 0, 0);");

        node.getChildren().addAll(rect, label);
        deskCanvas.getChildren().add(node);
        uiDeskMap.put(node, deskData);

        updateLabelRotation(node, deskData.getWidth(), deskData.getHeight());
        addInteractionLogic(node);
        selectDesk(node);
        return node;
    }

    private void addInteractionLogic(StackPane node) {
        node.setOnMouseClicked(event -> { selectDesk(node); event.consume(); });
        node.setOnMousePressed(event -> {
            selectDesk(node);
            startX = event.getSceneX() - node.getLayoutX(); startY = event.getSceneY() - node.getLayoutY();
            node.setCursor(Cursor.CLOSED_HAND);
            captureSnapshot(node);
        });
        node.setOnMouseDragged(event -> {
            double newX = event.getSceneX() - startX; double newY = event.getSceneY() - startY;
            newX = clamp(newX, 0, MAP_LIMIT - node.getWidth());
            newY = clamp(newY, 0, MAP_LIMIT - node.getHeight());
            node.setLayoutX(newX); node.setLayoutY(newY);
            Desk data = uiDeskMap.get(node); data.setX(newX); data.setY(newY);
        });
        node.setOnMouseReleased(event -> {
            node.setCursor(Cursor.HAND);
            if (node.getLayoutX() != tempOldX || node.getLayoutY() != tempOldY) pushModifyAction(node);
        });
    }

    private void selectDesk(StackPane node) {
        if (selectedNode != null && selectedNode != node) updateVisuals(selectedNode, false);
        selectedNode = node; updateVisuals(node, true);
        Desk data = uiDeskMap.get(node);
        propertiesBox.setDisable(false);
        if (data.getSize() != null) sizeSpinner.getValueFactory().setValue(data.getSize());
        widthSlider.setValue(data.getWidth()); heightSlider.setValue(data.getHeight());
        colorPicker.setValue(Color.web(data.getColorHex() != null ? data.getColorHex() : "#8e44ad"));
        updateRangePreview();
    }

    private void updateVisuals(StackPane node, boolean isSelected) {
        Rectangle rect = (Rectangle) node.getChildren().get(0);
        if (isSelected) { rect.setStroke(Color.RED); rect.setStrokeWidth(2); rect.setEffect(new DropShadow(5, Color.BLACK)); }
        else { rect.setStroke(Color.BLACK); rect.setStrokeWidth(1); rect.setEffect(null); }
    }

    private void updateLabelRotation(StackPane node, double width, double height) {
        if (node.getChildren().size() > 1) {
            Label label = (Label) node.getChildren().get(1);
            if (height > width * 1.5) label.setRotate(-90); else label.setRotate(0);
        }
    }

    private void updateRangePreview() {
        if (selectedNode == null) return;
        Desk data = uiDeskMap.get(selectedNode);
        if (data.getId() == null && data.getIdRange() == null) {
            int alloc = allocationSpinner.getValue();
            int start = tempBlockIndexCounter + 1;
            int end = start + alloc - 1;
            rangePreviewLabel.setText("Est. Range: " + start + "-" + end);
        } else {
            rangePreviewLabel.setText("Range: " + data.getIdRange());
        }
    }

    @FXML
    public void handleSaveChanges() {
        try {
            if (!desksToDelete.isEmpty()) {
                for (Desk d : desksToDelete) deskRepository.deleteDeskById(d.getId());
                desksToDelete.clear();
            }
            for (Map.Entry<StackPane, Desk> entry : uiDeskMap.entrySet()) {
                StackPane node = entry.getKey();
                Desk data = entry.getValue();
                Rectangle rect = (Rectangle) node.getChildren().get(0);
                data.setX(node.getLayoutX()); data.setY(node.getLayoutY());
                data.setWidth(rect.getWidth()); data.setHeight(rect.getHeight());
                data.setColorHex(toHexString((Color) rect.getFill()));
                data.setSize(sizeSpinner.getValue());

                if (data.getId() == null) {
                    int alloc = allocationSpinner.getValue();
                    if (data.getSize() > alloc) throw new RuntimeException("Size cannot be larger than Allocation!");
                    int start = tempBlockIndexCounter + 1; int end = start + alloc - 1;
                    data.setIdRange(start + "-" + end); data.setCurrentIdIndex(0);
                    tempBlockIndexCounter = end;
                    deskRepository.insertDesk(data.getSize(), data.getIdRange(), data.getCurrentIdIndex(), data.getX(), data.getY(), data.getWidth(), data.getHeight(), data.getColorHex(), currentBlock.getId());
                } else {
                    deskRepository.updateDesk(data.getId(), data.getSize(), data.getX(), data.getY(), data.getWidth(), data.getHeight(), data.getColorHex());
                }
            }
            blockRepository.updateBlockCurrentIndex(currentBlock.getId(), tempBlockIndexCounter);
            currentBlock.setCurrentIdIndex(tempBlockIndexCounter);
            undoStack.clear(); updateUndoButtonState(); loadDesksForBlock();
            showSuccess("Desks Saved!");
        } catch (Exception e) {
            e.printStackTrace();
            showSuccess("Error: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    public void handleDeleteDesk() {
        if (selectedNode != null) {
            Desk data = uiDeskMap.get(selectedNode);
            if (data.getId() != null) desksToDelete.add(data);
            undoStack.push(new HistoryAction(ActionType.DELETE, selectedNode, data));
            updateUndoButtonState();
            deskCanvas.getChildren().remove(selectedNode);
            uiDeskMap.remove(selectedNode);
            selectedNode = null;
            propertiesBox.setDisable(true);
        }
    }

    @FXML
    public void handleUndo() {
        if (undoStack.isEmpty()) return;
        HistoryAction action = undoStack.pop();
        if (action.type == ActionType.ADD) {
            deskCanvas.getChildren().remove(action.uiNode); uiDeskMap.remove(action.uiNode);
            selectedNode = null; propertiesBox.setDisable(true);
        } else if (action.type == ActionType.DELETE) {
            deskCanvas.getChildren().add(action.uiNode); uiDeskMap.put(action.uiNode, action.data);
            if (action.data.getId() != null) desksToDelete.remove(action.data);
            addInteractionLogic(action.uiNode); selectDesk(action.uiNode);
        } else if (action.type == ActionType.MODIFY) {
            StackPane node = action.uiNode;
            Rectangle rect = (Rectangle) node.getChildren().get(0);
            node.setLayoutX(action.prevX); node.setLayoutY(action.prevY);
            node.setPrefSize(action.prevW, action.prevH);
            rect.setWidth(action.prevW); rect.setHeight(action.prevH);
            Desk d = uiDeskMap.get(node); d.setX(action.prevX); d.setY(action.prevY); d.setWidth(action.prevW); d.setHeight(action.prevH);
            updateLabelRotation(node, action.prevW, action.prevH);
            if(selectedNode == node) { widthSlider.setValue(action.prevW); heightSlider.setValue(action.prevH); }
        }
        updateUndoButtonState();
    }

    private void setupEditorListeners() {
        sizeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 4));
        allocationSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 10));
        allocationSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateRangePreview());
        setupSliderUndoLogic(widthSlider, "WIDTH");
        // DÜZELTME: Genişlik sınırı kontrolü eklendi
        widthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedNode != null) {
                double newWidth = newVal.doubleValue();
                double currentX = selectedNode.getLayoutX();
                // Tuval dışına taşırmama kontrolü
                if (currentX + newWidth > MAP_LIMIT) newWidth = MAP_LIMIT - currentX;

                Rectangle rect = (Rectangle) selectedNode.getChildren().get(0);
                rect.setWidth(newWidth);
                selectedNode.setPrefWidth(newWidth);
                Desk data = uiDeskMap.get(selectedNode); data.setWidth(newWidth);
                updateLabelRotation(selectedNode, newWidth, rect.getHeight());
            }
        });

        setupSliderUndoLogic(heightSlider, "HEIGHT");
        // DÜZELTME: Yükseklik sınırı kontrolü eklendi
        heightSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedNode != null) {
                double newHeight = newVal.doubleValue();
                double currentY = selectedNode.getLayoutY();
                // Tuval dışına taşırmama kontrolü
                if (currentY + newHeight > MAP_LIMIT) newHeight = MAP_LIMIT - currentY;

                Rectangle rect = (Rectangle) selectedNode.getChildren().get(0);
                rect.setHeight(newHeight);
                selectedNode.setPrefHeight(newHeight);
                Desk data = uiDeskMap.get(selectedNode); data.setHeight(newHeight);
                updateLabelRotation(selectedNode, rect.getWidth(), newHeight);
            }
        });
        colorPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedNode != null) {
                Rectangle rect = (Rectangle) selectedNode.getChildren().get(0);
                rect.setFill(newVal); rect.setOpacity(0.8);
                Desk data = uiDeskMap.get(selectedNode); data.setColorHex(toHexString(newVal));
            }
        });
    }

    private void setupSliderUndoLogic(Slider slider, String type) {
        slider.setOnMousePressed(e -> { if(selectedNode != null) captureSnapshot(selectedNode); });
        slider.setOnMouseReleased(e -> { if(selectedNode != null) pushModifyAction(selectedNode); });
    }

    private void captureSnapshot(StackPane node) {
        Rectangle rect = (Rectangle) node.getChildren().get(0);
        tempOldX = node.getLayoutX(); tempOldY = node.getLayoutY();
        tempOldWidth = rect.getWidth(); tempOldHeight = rect.getHeight();
    }

    private void pushModifyAction(StackPane node) {
        Desk data = uiDeskMap.get(node);
        undoStack.push(new HistoryAction(ActionType.MODIFY, node, data, tempOldX, tempOldY, tempOldWidth, tempOldHeight));
        updateUndoButtonState();
    }

    private void updateUndoButtonState() { if (btnUndo != null) btnUndo.setDisable(undoStack.isEmpty()); }
    private double clamp(double val, double min, double max) { return Math.max(min, Math.min(val, max)); }
    private String toHexString(Color color) { return String.format("#%02X%02X%02X", (int)(color.getRed()*255), (int)(color.getGreen()*255), (int)(color.getBlue()*255)); }
    private void showSuccess(String msg) { statusLabel.setText(msg); statusLabel.setStyle("-fx-text-fill: #27ae60;"); statusLabel.setVisible(true); }
    private void showError(String msg) { statusLabel.setText(msg); statusLabel.setStyle("-fx-text-fill: red;"); statusLabel.setVisible(true); }
}