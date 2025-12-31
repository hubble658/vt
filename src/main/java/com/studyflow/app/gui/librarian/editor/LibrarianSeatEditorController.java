package com.studyflow.app.gui.librarian.editor;

import com.studyflow.app.gui.NavigationService;
import com.studyflow.app.gui.ViewFactory;
import com.studyflow.app.model.facility.Desk;
import com.studyflow.app.model.facility.Facility;
import com.studyflow.app.model.facility.FacilityBlock;
import com.studyflow.app.model.facility.Seat;
import com.studyflow.app.repository.facility.DeskRepository;
import com.studyflow.app.repository.facility.FacilityBlockRepository;
import com.studyflow.app.repository.facility.SeatRepository;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
public class LibrarianSeatEditorController {

    private static final double CANVAS_SIZE = 600.0;
    private static final double SEAT_SIZE   = 32.0;
    private static final double RAIL_GAP    = 8.0;  // masa ile koltuk arasindaki mesafe

    // --- FXML: LAYERS ---
    @FXML private ScrollPane facilityScroll;
    @FXML private Pane       facilityCanvas;

    @FXML private ScrollPane deskSelectionScroll;
    @FXML private Pane       deskSelectionCanvas;

    @FXML private StackPane  seatEditorContainer;
    @FXML private Pane       drawingSurface;

    // --- RIGHT SIDEBAR ---
    @FXML private VBox   sidebarInstructions;
    @FXML private VBox   sidebarEditor;
    @FXML private VBox   propertiesBox;

    @FXML private Label  lblSidebarTitle;
    @FXML private Label  lblSidebarDesc;
    @FXML private Button btnBackToBlocks;

    @FXML private Label  lblBreadcrumb;
    @FXML private Label  lblCurrentDeskInfo;
    @FXML private Label  lblSelectedSeatInfo;
    @FXML private Label  lblRangeInfo;

    @FXML private Spinner<Integer> seatNumSpinner;
    @FXML private Slider           posXSlider;
    @FXML private Slider           posYSlider;

    @FXML private Button btnUndo;
    @FXML private Button btnSave;
    @FXML private Label  statusLabel;
    @FXML private Button btnBack;

    // --- Repositories & Services ---
    @Autowired private FacilityBlockRepository blockRepo;
    @Autowired private DeskRepository          deskRepo;
    @Autowired private SeatRepository          seatRepo;
    @Autowired private UserSessionContext      userSessionContext;
    @Autowired private NavigationService navigationService;
    @Autowired private ViewFactory viewFactory;

    // --- State ---
    private FacilityBlock selectedBlock;
    private Desk          selectedDesk;

    private final Map<StackPane, Seat> uiSeatMap = new HashMap<>();
    private StackPane                  selectedSeatNode;

    // Masa (desk) ekranda ortalanmis halinin bilgileri
    private double deskOriginX;
    private double deskOriginY;
    private double deskWidth;
    private double deskHeight;

    // Drag state
    private double mouseAnchorX;
    private double mouseAnchorY;
    private double lastRelX;
    private double lastRelY;

    private enum EditorMode { SELECT_BLOCK, SELECT_DESK, EDIT_SEATS }
    private EditorMode currentMode = EditorMode.SELECT_BLOCK;

    private enum SeatSide { TOP, RIGHT, BOTTOM, LEFT }

    // UNDO
    private enum ActionType { ADD, DELETE, MOVE }

    private static class HistoryAction {
        ActionType type;
        StackPane  node;
        Seat       seat;
        double     prevRelX;
        double     prevRelY;

        HistoryAction(ActionType type, StackPane node, Seat seat) {
            this.type = type;
            this.node = node;
            this.seat = seat;
        }

        HistoryAction(ActionType type, StackPane node, Seat seat, double prevRelX, double prevRelY) {
            this.type     = type;
            this.node     = node;
            this.seat     = seat;
            this.prevRelX = prevRelX;
            this.prevRelY = prevRelY;
        }
    }

    private final Deque<HistoryAction> undoStack = new ArrayDeque<>();

    // ------------------------------------------------------------------------
    // INITIALIZE
    // ------------------------------------------------------------------------
    @FXML
    public void initialize() {
        facilityCanvas.setPrefSize(CANVAS_SIZE, CANVAS_SIZE);
        facilityCanvas.setMinSize(CANVAS_SIZE, CANVAS_SIZE);
        facilityCanvas.setMaxSize(CANVAS_SIZE, CANVAS_SIZE);

        deskSelectionCanvas.setPrefSize(CANVAS_SIZE, CANVAS_SIZE);
        deskSelectionCanvas.setMinSize(CANVAS_SIZE, CANVAS_SIZE);
        deskSelectionCanvas.setMaxSize(CANVAS_SIZE, CANVAS_SIZE);

        drawingSurface.setPrefSize(CANVAS_SIZE, CANVAS_SIZE);
        drawingSurface.setMinSize(CANVAS_SIZE, CANVAS_SIZE);
        drawingSurface.setMaxSize(CANVAS_SIZE, CANVAS_SIZE);

        posXSlider.setMin(0); posXSlider.setMax(CANVAS_SIZE);
        posYSlider.setMin(0); posYSlider.setMax(CANVAS_SIZE);

        setupPropertyListeners();
        showBlockSelectionLayer();
        updateUndoButtonState();
    }

    // ------------------------------------------------------------------------
    // LAYER 1: BLOCK SELECTION
    // ------------------------------------------------------------------------
    private void showBlockSelectionLayer() {
        currentMode   = EditorMode.SELECT_BLOCK;
        selectedBlock = null;
        selectedDesk  = null;
        uiSeatMap.clear();
        selectedSeatNode = null;
        undoStack.clear();
        updateUndoButtonState();

        toggleLayers(true, false, false);

        sidebarInstructions.setVisible(true);
        sidebarInstructions.setManaged(true);
        sidebarEditor.setVisible(false);
        sidebarEditor.setManaged(false);

        btnBack.setVisible(false);
        btnBack.setManaged(false);
        btnBackToBlocks.setVisible(false);
        btnBackToBlocks.setManaged(false);

        lblSidebarTitle.setText("Select a Block");
        lblSidebarDesc.setText("Click a block to choose its desks.");
        lblBreadcrumb.setText(" > Select a Block");

        loadBlocks();
    }

    private void loadBlocks() {
        facilityCanvas.getChildren().clear();

        Facility facility = userSessionContext.getCurrentUser().getFacilityInfo().getFacility();
        if (facility == null) return;

        blockRepo.findAllByFacilityId(facility.getId())
                .forEach(b -> facilityCanvas.getChildren().add(createBlockNode(b)));
    }

    private StackPane createBlockNode(FacilityBlock b) {
        StackPane node = new StackPane();
        node.setLayoutX(b.getX());
        node.setLayoutY(b.getY());
        node.setPrefSize(b.getWidth(), b.getHeight());
        node.setCursor(Cursor.HAND);

        Rectangle rect = new Rectangle(b.getWidth(), b.getHeight());
        try {
            rect.setFill(Color.web(b.getColorHex()));
        } catch (Exception e) {
            rect.setFill(Color.GRAY);
        }
        rect.setStroke(Color.BLACK);
        rect.setStrokeWidth(1);
        rect.setOpacity(0.9);
        rect.setEffect(new DropShadow(3, Color.rgb(0, 0, 0, 0.2)));

        Label label = new Label(b.getName());
        label.setStyle("-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 14px;" +
                "-fx-effect: dropshadow(one-pass-box, black, 2, 0.5, 0, 0);");

        node.getChildren().addAll(rect, label);

        node.setOnMouseEntered(e -> {
            rect.setStroke(Color.ORANGE);
            rect.setStrokeWidth(3);
            rect.setEffect(new DropShadow(8, Color.ORANGE));
        });
        node.setOnMouseExited(e -> {
            rect.setStroke(Color.BLACK);
            rect.setStrokeWidth(1);
            rect.setEffect(new DropShadow(3, Color.rgb(0, 0, 0, 0.2)));
        });

        node.setOnMouseClicked(e -> showDeskSelectionLayer(b));
        return node;
    }

    // ------------------------------------------------------------------------
    // LAYER 2: DESK SELECTION
    // ------------------------------------------------------------------------
    private void showDeskSelectionLayer(FacilityBlock block) {
        currentMode   = EditorMode.SELECT_DESK;
        selectedBlock = block;
        selectedDesk  = null;
        uiSeatMap.clear();
        selectedSeatNode = null;
        undoStack.clear();
        updateUndoButtonState();

        toggleLayers(false, true, false);

        sidebarInstructions.setVisible(true);
        sidebarInstructions.setManaged(true);
        sidebarEditor.setVisible(false);
        sidebarEditor.setManaged(false);

        btnBack.setVisible(true);
        btnBack.setManaged(true);
        btnBackToBlocks.setVisible(true);
        btnBackToBlocks.setManaged(true);

        lblSidebarTitle.setText(block.getName());
        lblSidebarDesc.setText("Select a desk to edit its seats.");
        lblBreadcrumb.setText(" > " + block.getName() + " > Select a Desk");

        loadDesksForSelection();
    }

    private void loadDesksForSelection() {
        deskSelectionCanvas.getChildren().clear();
        deskRepo.findAllByBlockId(selectedBlock.getId())
                .forEach(d -> deskSelectionCanvas.getChildren().add(createDeskNode(d)));
    }

    private StackPane createDeskNode(Desk d) {
        StackPane node = new StackPane();
        node.setLayoutX(d.getX());
        node.setLayoutY(d.getY());
        node.setPrefSize(d.getWidth(), d.getHeight());
        node.setCursor(Cursor.HAND);

        Rectangle rect = new Rectangle(d.getWidth(), d.getHeight());
        try {
            rect.setFill(Color.web(d.getColorHex()));
        } catch (Exception e) {
            rect.setFill(Color.web("#9b59b6"));
        }
        rect.setStroke(Color.BLACK);
        rect.setOpacity(0.9);
        rect.setEffect(new DropShadow(2, Color.rgb(0, 0, 0, 0.3)));

        String txt = d.getIdRange() != null ? d.getIdRange() : "Desk";
        Label label = new Label(txt);
        label.setStyle("-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-effect: dropshadow(one-pass-box, black, 2, 0.5, 0, 0);");
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);

        if (d.getHeight() > d.getWidth() * 1.5) {
            label.setRotate(-90);
        }

        node.getChildren().addAll(rect, label);

        node.setOnMouseEntered(e -> {
            rect.setStroke(Color.ORANGE);
            rect.setStrokeWidth(2);
            rect.setEffect(new DropShadow(5, Color.BLACK));
        });
        node.setOnMouseExited(e -> {
            rect.setStroke(Color.BLACK);
            rect.setStrokeWidth(1);
            rect.setEffect(new DropShadow(2, Color.rgb(0, 0, 0, 0.3)));
        });

        node.setOnMouseClicked(e -> showSeatEditorLayer(d));
        return node;
    }

    // ------------------------------------------------------------------------
    // LAYER 3: SEAT EDITOR
    // ------------------------------------------------------------------------
    private void showSeatEditorLayer(Desk desk) {
        currentMode  = EditorMode.EDIT_SEATS;
        selectedDesk = desk;
        uiSeatMap.clear();
        selectedSeatNode = null;
        undoStack.clear();
        updateUndoButtonState();

        toggleLayers(false, false, true);

        sidebarInstructions.setVisible(false);
        sidebarInstructions.setManaged(false);
        sidebarEditor.setVisible(true);
        sidebarEditor.setManaged(true);

        btnBack.setVisible(true);
        btnBack.setManaged(true);
        btnBackToBlocks.setVisible(true);
        btnBackToBlocks.setManaged(true);

        lblBreadcrumb.setText(" > " + selectedBlock.getName() + " > Editing: " + desk.getIdRange());
        lblCurrentDeskInfo.setText("Desk: " + desk.getIdRange());
        lblRangeInfo.setText("Allowed Range: " + desk.getIdRange());
        lblSelectedSeatInfo.setText("No Selection");
        propertiesBox.setDisable(true);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        loadSeatEditorCanvas();
    }

    private void loadSeatEditorCanvas() {
        drawingSurface.getChildren().clear();
        uiSeatMap.clear();
        selectedSeatNode = null;
        propertiesBox.setDisable(true);

        deskWidth  = selectedDesk.getWidth();
        deskHeight = selectedDesk.getHeight();
        deskOriginX = (CANVAS_SIZE - deskWidth) / 2.0;
        deskOriginY = (CANVAS_SIZE - deskHeight) / 2.0;

        Rectangle deskRect = new Rectangle(deskWidth, deskHeight);
        deskRect.setX(deskOriginX);
        deskRect.setY(deskOriginY);
        deskRect.setArcWidth(15);
        deskRect.setArcHeight(15);

        try {
            deskRect.setFill(Color.web(selectedDesk.getColorHex()));
        } catch (Exception e) {
            deskRect.setFill(Color.PURPLE);
        }

        deskRect.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.15)));
        deskRect.setStroke(Color.DARKGRAY);
        deskRect.setOpacity(0.5);

        Label deskLbl = new Label(selectedDesk.getIdRange());
        deskLbl.setStyle("-fx-font-size: 24px; -fx-text-fill: rgba(0,0,0,0.3); -fx-font-weight: bold;");
        deskLbl.setLayoutX(deskOriginX + (deskWidth / 2.0) - 30);
        deskLbl.setLayoutY(deskOriginY + (deskHeight / 2.0) - 15);

        drawingSurface.getChildren().addAll(deskRect, deskLbl);

        // Mevcut koltuklari rail'e snap ederek yukle
        List<Seat> seats = seatRepo.findAllByDeskId(selectedDesk.getId());
        for (Seat seat : seats) {
            if (seat.getRelX() == null) seat.setRelX(0.0);
            if (seat.getRelY() == null) seat.setRelY(0.0);
            addVisualSeatNode(seat);
        }
    }

    // ------------------------------------------------------------------------
    // SEAT NODE OLUŞTURMA & SEÇİM & DRAG (RAIL LOGIC)
    // ------------------------------------------------------------------------
    private StackPane addVisualSeatNode(Seat seatData) {
        double relX = seatData.getRelX() != null ? seatData.getRelX() : 0.0;
        double relY = seatData.getRelY() != null ? seatData.getRelY() : 0.0;
        double absX = deskOriginX + relX;
        double absY = deskOriginY + relY;

        double[] snapped = snapToRail(absX, absY);
        absX = snapped[0];
        absY = snapped[1];

        seatData.setRelX(absX - deskOriginX);
        seatData.setRelY(absY - deskOriginY);

        StackPane seatNode = new StackPane();
        seatNode.setLayoutX(absX);
        seatNode.setLayoutY(absY);
        seatNode.setPrefSize(SEAT_SIZE, SEAT_SIZE);

        Rectangle r = new Rectangle(SEAT_SIZE, SEAT_SIZE);
        r.setArcWidth(10);
        r.setArcHeight(10);
        r.setFill(Color.web("#27ae60"));
        r.setStroke(Color.web("#1e8449"));
        r.setStrokeWidth(1.5);
        r.setEffect(new DropShadow(4, Color.rgb(0, 0, 0, 0.3)));

        Label l = new Label(String.valueOf(seatData.getSeatNumber()));
        l.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-effect: dropshadow(one-pass-box, black, 2, 0.5, 0, 0);");

        seatNode.getChildren().addAll(r, l);
        seatNode.setCursor(Cursor.OPEN_HAND);

        uiSeatMap.put(seatNode, seatData);

        seatNode.setOnMouseClicked(e -> {
            selectSeat(seatNode);
            e.consume();
        });

        seatNode.setOnMousePressed(e -> {
            selectSeat(seatNode);
            seatNode.setCursor(Cursor.CLOSED_HAND);
            seatNode.toFront();
            mouseAnchorX = e.getSceneX() - seatNode.getLayoutX();
            mouseAnchorY = e.getSceneY() - seatNode.getLayoutY();

            Seat s = uiSeatMap.get(seatNode);
            if (s != null) {
                lastRelX = s.getRelX();
                lastRelY = s.getRelY();
            }
            e.consume();
        });

        seatNode.setOnMouseDragged(e -> {
            double desiredX = e.getSceneX() - mouseAnchorX;
            double desiredY = e.getSceneY() - mouseAnchorY;

            double[] snappedPos = snapToRail(desiredX, desiredY);
            double newX = snappedPos[0];
            double newY = snappedPos[1];

            seatNode.setLayoutX(newX);
            seatNode.setLayoutY(newY);

            Seat s = uiSeatMap.get(seatNode);
            if (s != null) {
                s.setRelX(newX - deskOriginX);
                s.setRelY(newY - deskOriginY);
            }

            if (!posXSlider.isPressed()) posXSlider.setValue(newX);
            if (!posYSlider.isPressed()) posYSlider.setValue(newY);
        });

        seatNode.setOnMouseReleased(e -> {
            seatNode.setCursor(Cursor.OPEN_HAND);
            Seat s = uiSeatMap.get(seatNode);
            if (s != null && (s.getRelX() != lastRelX || s.getRelY() != lastRelY)) {
                undoStack.push(new HistoryAction(ActionType.MOVE, seatNode, s, lastRelX, lastRelY));
                updateUndoButtonState();
            }
        });

        drawingSurface.getChildren().add(seatNode);
        return seatNode;
    }

    private void selectSeat(StackPane node) {
        if (selectedSeatNode != null && selectedSeatNode != node) {
            Rectangle oldR = (Rectangle) selectedSeatNode.getChildren().get(0);
            oldR.setStroke(Color.web("#1e8449"));
            oldR.setStrokeWidth(1.5);
            oldR.setEffect(new DropShadow(4, Color.rgb(0, 0, 0, 0.3)));
        }

        selectedSeatNode = node;
        Seat data = uiSeatMap.get(node);
        if (data == null) return;

        Rectangle r = (Rectangle) node.getChildren().get(0);
        r.setStroke(Color.GOLD);
        r.setStrokeWidth(3);
        r.setEffect(new DropShadow(8, Color.GOLD));

        propertiesBox.setDisable(false);
        lblSelectedSeatInfo.setText("Editing Seat #" + data.getSeatNumber());

        try {
            String[] parts = selectedDesk.getIdRange().split("-");
            int min = Integer.parseInt(parts[0].trim());
            int max = Integer.parseInt(parts[1].trim());

            Supplier<Set<Integer>> usedSupplier = () -> uiSeatMap.values().stream()
                    .map(Seat::getSeatNumber)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            SeatIdSpinnerValueFactory factory = new SeatIdSpinnerValueFactory(min, max, data.getSeatNumber(), data, usedSupplier);
            seatNumSpinner.setValueFactory(factory);
        } catch (Exception ex) {
            seatNumSpinner.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 9999, data.getSeatNumber())
            );
        }

        double absX = deskOriginX + data.getRelX();
        double absY = deskOriginY + data.getRelY();
        posXSlider.setValue(absX);
        posYSlider.setValue(absY);
    }

    // ------------------------------------------------------------------------
    // CUSTOM SEAT ID SPINNER (ARROWS SKIP TAKEN IDS)
    // ------------------------------------------------------------------------
    private static class SeatIdSpinnerValueFactory extends SpinnerValueFactory.IntegerSpinnerValueFactory {
        private final Seat currentSeat;
        private final Supplier<Set<Integer>> usedSupplier;

        SeatIdSpinnerValueFactory(int min, int max, int initialValue,
                                  Seat currentSeat,
                                  Supplier<Set<Integer>> usedSupplier) {
            super(min, max, initialValue);
            this.currentSeat  = currentSeat;
            this.usedSupplier = usedSupplier;
        }

        @Override
        public void increment(int steps) {
            if (steps <= 0) return;
            Integer value = getValue();
            if (value == null) value = getMin();
            for (int i = 0; i < steps; i++) {
                value = nextAvailable(value + 1, +1);
            }
            setValue(value);
        }

        @Override
        public void decrement(int steps) {
            if (steps <= 0) return;
            Integer value = getValue();
            if (value == null) value = getMin();
            for (int i = 0; i < steps; i++) {
                value = nextAvailable(value - 1, -1);
            }
            setValue(value);
        }

        private int nextAvailable(int start, int dir) {
            int min = getMin();
            int max = getMax();

            Set<Integer> used = new HashSet<>(usedSupplier.get());
            if (currentSeat != null && currentSeat.getSeatNumber() != null) {
                used.remove(currentSeat.getSeatNumber());
            }

            int v = start;
            while (v >= min && v <= max && used.contains(v)) {
                v += dir;
            }
            if (v < min || v > max) {
                Integer cur = getValue();
                return cur != null ? cur : min;
            }
            return v;
        }
    }

    // ------------------------------------------------------------------------
    // ADD NEW SEAT (RAIL UZERINDE OTOMATIK DIZIM)
    // ------------------------------------------------------------------------
    @FXML
    public void handleAddNewSeat() {
        if (selectedDesk == null) {
            showError("Please select a desk first.");
            return;
        }

        Integer nextId = findNextAvailableSeatId();
        if (nextId == null) {
            showError("No available IDs in range " + selectedDesk.getIdRange());
            return;
        }

        int index = uiSeatMap.size();
        SeatSide[] sides = SeatSide.values();
        SeatSide side = sides[index % sides.length];
        int indexOnSide = index / sides.length;

        double gap    = SEAT_SIZE + 8.0;
        double margin = 16.0;
        double absX;
        double absY;

        switch (side) {
            case TOP:
                absX = deskOriginX + margin + indexOnSide * gap;
                absY = deskOriginY - SEAT_SIZE - RAIL_GAP;
                break;
            case RIGHT:
                absX = deskOriginX + deskWidth + RAIL_GAP;
                absY = deskOriginY + margin + indexOnSide * gap;
                break;
            case BOTTOM:
                absX = deskOriginX + margin + indexOnSide * gap;
                absY = deskOriginY + deskHeight + RAIL_GAP;
                break;
            case LEFT:
            default:
                absX = deskOriginX - SEAT_SIZE - RAIL_GAP;
                absY = deskOriginY + margin + indexOnSide * gap;
                break;
        }

        double[] snapped = snapToRail(absX, absY);
        absX = snapped[0];
        absY = snapped[1];

        Seat newSeat = new Seat();
        newSeat.setSeatNumber(nextId);
        newSeat.setRelX(absX - deskOriginX);
        newSeat.setRelY(absY - deskOriginY);
        newSeat.setDesk(selectedDesk);

        StackPane node = addVisualSeatNode(newSeat);
        undoStack.push(new HistoryAction(ActionType.ADD, node, newSeat));
        updateUndoButtonState();

        selectSeat(node);
        showSuccess("Seat " + nextId + " added around desk. Drag to adjust.");
    }

    // ------------------------------------------------------------------------
    // DELETE SEAT (SADECE YENI / HENUZ DB'DE OLMAYAN)
    // ------------------------------------------------------------------------
    @FXML
    public void handleDeleteSeat() {
        if (selectedSeatNode == null) {
            showError("No seat selected.");
            return;
        }
        Seat s = uiSeatMap.get(selectedSeatNode);
        if (s == null) return;

        if (s.getId() != null) {
            showError("This seat already exists in the system and cannot be deleted here.");
            return;
        }

        undoStack.push(new HistoryAction(ActionType.DELETE, selectedSeatNode, s));
        updateUndoButtonState();

        drawingSurface.getChildren().remove(selectedSeatNode);
        uiSeatMap.remove(selectedSeatNode);
        selectedSeatNode = null;
        propertiesBox.setDisable(true);
        lblSelectedSeatInfo.setText("No Selection");
        showSuccess("Seat removed from layout (not yet saved).");
    }

    // ------------------------------------------------------------------------
    // PROPERTY LISTENERS
    // ------------------------------------------------------------------------
    private void setupPropertyListeners() {
        if (seatNumSpinner != null) {
            seatNumSpinner.valueProperty().addListener((obs, oldV, newV) -> {
                if (selectedSeatNode != null && newV != null) {
                    Seat current = uiSeatMap.get(selectedSeatNode);
                    if (current == null) return;

                    boolean taken = uiSeatMap.values().stream()
                            .filter(s -> s != current)
                            .anyMatch(s -> Objects.equals(s.getSeatNumber(), newV));

                    if (taken) {
                        showError("Seat ID " + newV + " is already used on this desk.");
                        if (seatNumSpinner.getValueFactory() != null) {
                            javafx.application.Platform.runLater(
                                    () -> seatNumSpinner.getValueFactory().setValue(oldV)
                            );
                        }
                    } else {
                        current.setSeatNumber(newV);
                        ((Label) selectedSeatNode.getChildren().get(1))
                                .setText(String.valueOf(newV));
                        showSuccess("Seat ID updated.");
                    }
                }
            });
        }

        posXSlider.valueProperty().addListener((obs, o, n) -> {
            if (selectedSeatNode != null && posXSlider.isPressed()) {
                double desiredX = n.doubleValue();
                double desiredY = selectedSeatNode.getLayoutY();
                double[] snapped = snapToRail(desiredX, desiredY);
                double newX = snapped[0];
                double newY = snapped[1];

                selectedSeatNode.setLayoutX(newX);
                selectedSeatNode.setLayoutY(newY);

                if (!posYSlider.isPressed()) posYSlider.setValue(newY);

                Seat s = uiSeatMap.get(selectedSeatNode);
                if (s != null) {
                    s.setRelX(newX - deskOriginX);
                    s.setRelY(newY - deskOriginY);
                }
            }
        });

        posYSlider.valueProperty().addListener((obs, o, n) -> {
            if (selectedSeatNode != null && posYSlider.isPressed()) {
                double desiredY = n.doubleValue();
                double desiredX = selectedSeatNode.getLayoutX();
                double[] snapped = snapToRail(desiredX, desiredY);
                double newX = snapped[0];
                double newY = snapped[1];

                selectedSeatNode.setLayoutX(newX);
                selectedSeatNode.setLayoutY(newY);

                if (!posXSlider.isPressed()) posXSlider.setValue(newX);

                Seat s = uiSeatMap.get(selectedSeatNode);
                if (s != null) {
                    s.setRelX(newX - deskOriginX);
                    s.setRelY(newY - deskOriginY);
                }
            }
        });
    }

    // ------------------------------------------------------------------------
    // SAVE
    // ------------------------------------------------------------------------
    @FXML
    public void handleSaveChanges() {
        try {
            for (Seat s : uiSeatMap.values()) {
                if (s.getId() == null) {
                    seatRepo.insertSeatWithCoordinates(
                            selectedDesk.getId(),
                            s.getSeatNumber(),
                            s.getRelX(),
                            s.getRelY()
                    );
                } else {
                    seatRepo.updateSeat(
                            s.getId(),
                            s.getSeatNumber(),
                            s.getRelX(),
                            s.getRelY()
                    );
                }
            }

            undoStack.clear();
            updateUndoButtonState();

            showSuccess("Seat layout saved successfully.");
            loadSeatEditorCanvas();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Save failed: " + e.getMessage());
        }
    }

    private Integer findNextAvailableSeatId() {
        try {
            String[] parts = selectedDesk.getIdRange().split("-");
            int start = Integer.parseInt(parts[0].trim());
            int end   = Integer.parseInt(parts[1].trim());

            Set<Integer> used = uiSeatMap.values().stream()
                    .map(Seat::getSeatNumber)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            for (int i = start; i <= end; i++) {
                if (!used.contains(i)) return i;
            }
            return null;
        } catch (Exception e) {
            return 1;
        }
    }

    // ------------------------------------------------------------------------
    // RAIL SNAP LOGIC
    // ------------------------------------------------------------------------
    private double[] snapToRail(double desiredX, double desiredY) {
        double topY    = deskOriginY - SEAT_SIZE - RAIL_GAP;
        double bottomY = deskOriginY + deskHeight + RAIL_GAP;
        double leftX   = deskOriginX - SEAT_SIZE - RAIL_GAP;
        double rightX  = deskOriginX + deskWidth + RAIL_GAP;

        double dTop    = Math.abs(desiredY - topY);
        double dBottom = Math.abs(desiredY - bottomY);
        double dLeft   = Math.abs(desiredX - leftX);
        double dRight  = Math.abs(desiredX - rightX);

        double absX;
        double absY;

        if (dTop <= dBottom && dTop <= dLeft && dTop <= dRight) {
            absY = topY;
            absX = clamp(desiredX, deskOriginX, deskOriginX + deskWidth - SEAT_SIZE);
        } else if (dBottom <= dTop && dBottom <= dLeft && dBottom <= dRight) {
            absY = bottomY;
            absX = clamp(desiredX, deskOriginX, deskOriginX + deskWidth - SEAT_SIZE);
        } else if (dLeft <= dRight && dLeft <= dTop && dLeft <= dBottom) {
            absX = leftX;
            absY = clamp(desiredY, deskOriginY, deskOriginY + deskHeight - SEAT_SIZE);
        } else {
            absX = rightX;
            absY = clamp(desiredY, deskOriginY, deskOriginY + deskHeight - SEAT_SIZE);
        }

        absX = clamp(absX, 0, CANVAS_SIZE - SEAT_SIZE);
        absY = clamp(absY, 0, CANVAS_SIZE - SEAT_SIZE);

        return new double[]{absX, absY};
    }

    // ------------------------------------------------------------------------
    // UNDO
    // ------------------------------------------------------------------------
    @FXML
    public void handleUndo() {
        if (undoStack.isEmpty()) return;

        HistoryAction action = undoStack.pop();
        switch (action.type) {
            case ADD:
                drawingSurface.getChildren().remove(action.node);
                uiSeatMap.remove(action.node);
                if (selectedSeatNode == action.node) {
                    selectedSeatNode = null;
                    propertiesBox.setDisable(true);
                    lblSelectedSeatInfo.setText("No Selection");
                }
                break;
            case DELETE:
                drawingSurface.getChildren().add(action.node);
                uiSeatMap.put(action.node, action.seat);
                // event handlerlar node'un uzerinde zaten duruyor
                selectSeat(action.node);
                break;
            case MOVE:
                Seat s = action.seat;
                s.setRelX(action.prevRelX);
                s.setRelY(action.prevRelY);
                double absX = deskOriginX + action.prevRelX;
                double absY = deskOriginY + action.prevRelY;
                action.node.setLayoutX(absX);
                action.node.setLayoutY(absY);
                if (selectedSeatNode == action.node) {
                    posXSlider.setValue(absX);
                    posYSlider.setValue(absY);
                }
                break;
        }

        updateUndoButtonState();
    }

    private void updateUndoButtonState() {
        if (btnUndo != null) {
            btnUndo.setDisable(undoStack.isEmpty());
        }
    }

    // ------------------------------------------------------------------------
    // NAVIGATION & HELPERS
    // ------------------------------------------------------------------------
    private void toggleLayers(boolean showBlocks, boolean showDesks, boolean showSeats) {
        facilityScroll.setVisible(showBlocks);
        facilityScroll.setManaged(showBlocks);

        deskSelectionScroll.setVisible(showDesks);
        deskSelectionScroll.setManaged(showDesks);

        seatEditorContainer.setVisible(showSeats);
        seatEditorContainer.setManaged(showSeats);
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private void showSuccess(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: #27ae60;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: #e74c3c;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    @FXML
    public void handleBackToBlocks() {
        showBlockSelectionLayer();
    }

    @FXML
    public void handleBack() {
        if (currentMode == EditorMode.EDIT_SEATS) {
            showDeskSelectionLayer(selectedBlock);
        } else if (currentMode == EditorMode.SELECT_DESK) {
            showBlockSelectionLayer();
        }
    }

    @FXML
    public void handleExit() {
        navigationService.navigateTo(viewFactory.loadView("/fxml/librarian/librarian-main-layout.fxml"));
    }
}