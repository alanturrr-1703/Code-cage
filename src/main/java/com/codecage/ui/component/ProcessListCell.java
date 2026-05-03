package com.codecage.ui.component;

import com.codecage.model.ExecutionStatus;
import com.codecage.model.ProcessRecord;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class ProcessListCell extends ListCell<ProcessRecord> {

    private final HBox root       = new HBox(8);
    private final Circle badge    = new Circle(6);
    private final VBox textBox    = new VBox(2);
    private final Label langLabel = new Label();
    private final Label prevLabel = new Label();
    private final Label infoLabel = new Label();

    public ProcessListCell() {
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(4, 8, 4, 8));
        root.getStyleClass().add("process-cell");

        langLabel.getStyleClass().add("process-lang");
        prevLabel.getStyleClass().add("process-preview");
        prevLabel.setMaxWidth(180);
        infoLabel.getStyleClass().add("process-info");

        HBox topRow = new HBox(6, langLabel, infoLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);
        textBox.getChildren().addAll(topRow, prevLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        root.getChildren().addAll(badge, textBox);
        setGraphic(null);
    }

    @Override
    protected void updateItem(ProcessRecord item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            return;
        }
        bind(item);
        setGraphic(root);
        setText(null);
    }

    private void bind(ProcessRecord rec) {
        updateBadge(rec.getStatus());
        langLabel.setText("#" + rec.getId() + "  " + rec.getLanguage().toUpperCase());
        prevLabel.setText(rec.getCodePreview());
        updateInfo(rec);

        // Live status changes
        rec.statusProperty().addListener((obs, o, n) -> {
            updateBadge(n);
            updateInfo(rec);
        });
        rec.executionTimeMsProperty().addListener((obs, o, n) -> updateInfo(rec));
    }

    private void updateBadge(ExecutionStatus status) {
        try {
            badge.setFill(Color.web(status.getColor()));
        } catch (Exception e) {
            badge.setFill(Color.GRAY);
        }
        // Animate running state
        if (status == ExecutionStatus.RUNNING) {
            badge.setStyle("-fx-effect: dropshadow(gaussian, " + status.getColor() + ", 8, 0.8, 0, 0);");
        } else {
            badge.setStyle("");
        }
    }

    private void updateInfo(ProcessRecord rec) {
        ExecutionStatus s = rec.getStatus();
        long ms = rec.getExecutionTimeMs();
        String timeStr = ms > 0 ? String.format("%.2fs", ms / 1000.0) : rec.getStartTime();
        infoLabel.setText(s.getLabel() + "  " + timeStr);
        infoLabel.setStyle("-fx-text-fill: " + s.getColor() + ";");
    }
}
