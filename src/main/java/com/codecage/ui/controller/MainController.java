package com.codecage.ui.controller;

import com.codecage.model.*;
import com.codecage.sandbox.DockerAvailability;
import com.codecage.sandbox.DockerImagePuller;
import com.codecage.sandbox.SandboxType;
import com.codecage.service.ExecutionService;
import com.codecage.service.ProcessManager;
import com.codecage.ui.component.ProcessListCell;
import com.codecage.ui.component.SyntaxHighlighter;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.ExecutorService;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

public class MainController {

    // ── Services ─────────────────────────────────────────────────────────────
    private final ProcessManager processManager = ProcessManager.getInstance();
    private final ExecutorService bgPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "exec-pool");
        t.setDaemon(true);
        return t;
    });

    // ── UI Components ─────────────────────────────────────────────────────────
    private TabPane editorTabPane;
    private ListView<ProcessRecord> processListView;
    private TextArea outputArea;
    private TextArea errorArea;
    private TextArea inputArea;
    private Label statusLabel;
    private Label timeBadge;
    private ComboBox<Language> langCombo;
    private Spinner<Integer> timeLimitSpinner;
    private Spinner<Integer> memLimitSpinner;
    private Button runBtn;
    private Button killBtn;
    private ToggleButton sandboxToggle;
    private int tabCounter = 1;

    // ── Listeners we need to un-bind ─────────────────────────────────────────
    private ChangeListener<String> outputListener;
    private ChangeListener<String> errorListener;
    private ChangeListener<ExecutionStatus> statusListener;

    // ── Previously bound record (for cleanup) ────────────────────────────────
    private ProcessRecord boundRecord;

    // =========================================================================
    //  Build the entire scene graph
    // =========================================================================
    public BorderPane buildRoot() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        root.setTop(buildToolBar());
        root.setCenter(buildCenter());
        root.setBottom(buildStatusBar());

        return root;
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────
    private Node buildToolBar() {
        // Language selector
        langCombo = new ComboBox<>();
        langCombo.getItems().addAll(Language.values());
        langCombo.setValue(Language.PYTHON);
        langCombo.getStyleClass().add("toolbar-combo");

        // Time limit
        timeLimitSpinner = new Spinner<>(500, 30000, 5000, 500);
        timeLimitSpinner.setPrefWidth(95);
        timeLimitSpinner.setEditable(true);
        timeLimitSpinner.getStyleClass().add("toolbar-spinner");

        // Memory limit
        memLimitSpinner = new Spinner<>(32, 2048, 256, 32);
        memLimitSpinner.setPrefWidth(85);
        memLimitSpinner.setEditable(true);
        memLimitSpinner.getStyleClass().add("toolbar-spinner");

        // Buttons
        Button newTabBtn = styledBtn("+ New Tab", "btn-new");
        runBtn = styledBtn("▶  Run", "btn-run");
        killBtn = styledBtn("■  Kill", "btn-kill");
        Button stopAllBtn = styledBtn("⏹ Stop All", "btn-stop");
        Button clearBtn = styledBtn("🗑 Clear", "btn-clear");

        // Sandbox toggle
        sandboxToggle = new ToggleButton(SandboxType.NONE.getLabel());
        sandboxToggle.getStyleClass().addAll("toolbar-btn", "btn-sandbox-off");
        sandboxToggle.setDisable(!DockerAvailability.isAvailable());
        if (!DockerAvailability.isAvailable()) {
            Tooltip tip = new Tooltip(
                "Docker daemon not found — sandbox unavailable"
            );
            Tooltip.install(sandboxToggle, tip);
            sandboxToggle.setText("🐳 No Docker");
        }
        sandboxToggle
            .selectedProperty()
            .addListener((obs, wasOn, isOn) -> handleSandboxToggle(isOn));

        newTabBtn.setOnAction(e -> addEditorTab(null));
        runBtn.setOnAction(e -> handleRun());
        killBtn.setOnAction(e -> handleKillSelected());
        stopAllBtn.setOnAction(e -> processManager.killAll());
        clearBtn.setOnAction(e -> processManager.clearCompleted());

        // Language change → update current editor template
        langCombo
            .valueProperty()
            .addListener((obs, old, lang) -> {
                CodeArea ca = getCurrentCodeArea();
                if (ca != null && ca.getText().isBlank()) {
                    ca.replaceText(lang.getTemplate());
                }
                updateHighlighting(ca, lang.getId());
            });

        Separator sep1 = new Separator(Orientation.VERTICAL);
        Separator sep2 = new Separator(Orientation.VERTICAL);
        Separator sep3 = new Separator(Orientation.VERTICAL);

        HBox bar = new HBox(
            8,
            newTabBtn,
            sep1,
            new Label("Lang:"),
            langCombo,
            new Label("⏱"),
            timeLimitSpinner,
            new Label("ms"),
            new Label("💾"),
            memLimitSpinner,
            new Label("MB"),
            sep2,
            runBtn,
            killBtn,
            stopAllBtn,
            clearBtn,
            sep3,
            sandboxToggle
        );
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 12, 6, 12));
        bar.getStyleClass().add("toolbar");
        return bar;
    }

    // ── Center split pane ─────────────────────────────────────────────────────
    private Node buildCenter() {
        // Left: process list
        processListView = new ListView<>(processManager.getRecords());
        processListView.setCellFactory(lv -> new ProcessListCell());
        processListView.getStyleClass().add("process-list");

        Label procHeader = new Label("  PROCESS HISTORY");
        procHeader.getStyleClass().add("sidebar-header");

        VBox sidebar = new VBox(0, procHeader, processListView);
        VBox.setVgrow(processListView, Priority.ALWAYS);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(240);
        sidebar.setMinWidth(180);

        // Center: editor tabs + bottom IO panel
        editorTabPane = new TabPane();
        editorTabPane.getStyleClass().add("editor-tabs");
        addEditorTab(null); // start with one tab

        SplitPane ioPane = buildIOPane();
        ioPane.setMinHeight(180);
        ioPane.setPrefHeight(220);

        VBox centerCol = new VBox(0, editorTabPane, ioPane);
        VBox.setVgrow(editorTabPane, Priority.ALWAYS);

        SplitPane mainSplit = new SplitPane(sidebar, centerCol);
        mainSplit.setDividerPositions(0.22);
        SplitPane.setResizableWithParent(sidebar, false);

        // Bind selection: show selected record's output
        processListView
            .getSelectionModel()
            .selectedItemProperty()
            .addListener((obs, old, rec) -> bindRecord(rec));

        // Auto-select new records
        processManager
            .getRecords()
            .addListener(
                (ListChangeListener<ProcessRecord>) c -> {
                    while (c.next()) {
                        if (c.wasAdded()) {
                            ProcessRecord latest = c
                                .getAddedSubList()
                                .get(c.getAddedSubList().size() - 1);
                            Platform.runLater(() ->
                                processListView
                                    .getSelectionModel()
                                    .select(latest)
                            );
                        }
                    }
                }
            );

        return mainSplit;
    }

    // ── IO pane (Input + Output/Error tabs) ───────────────────────────────────
    private SplitPane buildIOPane() {
        // Input
        inputArea = new TextArea();
        inputArea.setPromptText("stdin — type program input here…");
        inputArea.getStyleClass().add("io-area");
        inputArea.setWrapText(false);

        Label inLabel = new Label("  INPUT");
        inLabel.getStyleClass().add("io-header");
        VBox inputPane = new VBox(0, inLabel, inputArea);
        VBox.setVgrow(inputArea, Priority.ALWAYS);

        // Output
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(false);
        outputArea.getStyleClass().addAll("io-area", "output-area");

        // Error
        errorArea = new TextArea();
        errorArea.setEditable(false);
        errorArea.setWrapText(false);
        errorArea.getStyleClass().addAll("io-area", "error-area");

        // Info tab
        timeBadge = new Label("—");
        timeBadge.getStyleClass().add("info-value");
        Label statusInfoLabel = new Label("—");
        statusInfoLabel.getStyleClass().add("info-value");
        Label exitCodeLabel = new Label("—");
        exitCodeLabel.getStyleClass().add("info-value");

        VBox infoBox = new VBox(
            8,
            labeledRow("Status :", statusInfoLabel),
            labeledRow("Time   :", timeBadge),
            labeledRow("Exit   :", exitCodeLabel)
        );
        infoBox.setPadding(new Insets(12));
        infoBox.getStyleClass().add("info-pane");

        Tab outTab = closableTab("Output", outputArea);
        Tab errTab = closableTab("Errors", errorArea);
        Tab infoTab = closableTab("Info", infoBox);

        TabPane resultTabs = new TabPane(outTab, errTab, infoTab);
        resultTabs.getStyleClass().add("result-tabs");

        SplitPane io = new SplitPane(inputPane, resultTabs);
        io.setDividerPositions(0.3);
        return io;
    }

    private HBox labeledRow(String lbl, Label val) {
        Label key = new Label(lbl);
        key.getStyleClass().add("info-key");
        HBox row = new HBox(8, key, val);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ── Status bar ────────────────────────────────────────────────────────────
    private Node buildStatusBar() {
        statusLabel = new Label("Ready");
        statusLabel.getStyleClass().add("status-label");
        HBox bar = new HBox(statusLabel);
        bar.setPadding(new Insets(3, 12, 3, 12));
        bar.getStyleClass().add("status-bar");
        return bar;
    }

    // =========================================================================
    //  Editor tab management
    // =========================================================================
    private void addEditorTab(String code) {
        Language lang =
            langCombo != null ? langCombo.getValue() : Language.PYTHON;

        CodeArea codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.getStyleClass().add("code-area");
        codeArea.setWrapText(false);

        String initialCode = (code != null && !code.isBlank())
            ? code
            : lang.getTemplate();
        codeArea.replaceText(initialCode);

        // Live syntax highlighting (debounced)
        codeArea
            .multiPlainChanges()
            .successionEnds(Duration.ofMillis(120))
            .subscribe(ignore -> {
                Language cur =
                    langCombo != null ? langCombo.getValue() : Language.PYTHON;
                updateHighlighting(codeArea, cur.getId());
            });

        updateHighlighting(codeArea, lang.getId());

        String tabTitle = "Tab " + tabCounter++;
        Tab tab = new Tab(tabTitle, codeArea);
        tab.setClosable(true);

        editorTabPane.getTabs().add(tab);
        editorTabPane.getSelectionModel().select(tab);
    }

    private CodeArea getCurrentCodeArea() {
        Tab t =
            editorTabPane != null
                ? editorTabPane.getSelectionModel().getSelectedItem()
                : null;
        if (t == null) return null;
        Node content = t.getContent();
        return (content instanceof CodeArea ca) ? ca : null;
    }

    private void updateHighlighting(CodeArea ca, String langId) {
        if (ca == null) return;
        try {
            ca.setStyleSpans(
                0,
                SyntaxHighlighter.computeHighlighting(langId, ca.getText())
            );
        } catch (Exception ignored) {}
    }

    // =========================================================================
    //  Run / Kill handlers
    // =========================================================================
    private void handleRun() {
        CodeArea ca = getCurrentCodeArea();
        if (ca == null || ca.getText().isBlank()) {
            setStatus("Nothing to run — write some code first.");
            return;
        }

        Language lang = langCombo.getValue();
        String code = ca.getText();
        String input = inputArea.getText();

        RunRequest req = RunRequest.builder()
            .code(code)
            .language(lang.getId())
            .timeLimitMs(timeLimitSpinner.getValue())
            .memoryLimitMb(memLimitSpinner.getValue())
            .input(input)
            .sandboxed(sandboxToggle != null && sandboxToggle.isSelected())
            .build();

        ProcessRecord rec = processManager.createRecord(lang.getId(), code);

        ExecutionService svc = new ExecutionService(req, rec);
        svc.setExecutor(bgPool);
        svc.setOnSucceeded(e ->
            setStatus(
                "Run #" +
                    rec.getId() +
                    " finished — " +
                    rec.getStatus().getLabel() +
                    " in " +
                    String.format("%.2fs", rec.getExecutionTimeMs() / 1000.0)
            )
        );
        svc.setOnFailed(e ->
            setStatus("Run #" + rec.getId() + " failed unexpectedly.")
        );
        svc.start();

        setStatus(
            "Running #" + rec.getId() + " (" + lang.getDisplayName() + ")…"
        );
    }

    private void handleKillSelected() {
        ProcessRecord rec = processListView
            .getSelectionModel()
            .getSelectedItem();
        if (rec != null) rec.kill();
    }

    private void handleSandboxToggle(boolean enabled) {
        if (enabled) {
            sandboxToggle.setText(SandboxType.DOCKER.getLabel());
            sandboxToggle.getStyleClass().removeAll("btn-sandbox-off");
            sandboxToggle.getStyleClass().add("btn-sandbox-on");
            setStatus(
                "🔒 Sandbox ON — pre-pulling Docker images in background…"
            );
            DockerImagePuller.pullAllAsync(msg -> setStatus(msg));
        } else {
            sandboxToggle.setText(SandboxType.NONE.getLabel());
            sandboxToggle.getStyleClass().removeAll("btn-sandbox-on");
            sandboxToggle.getStyleClass().add("btn-sandbox-off");
            setStatus("🔓 Sandbox OFF — running natively");
        }
    }

    // =========================================================================
    //  Bind selected ProcessRecord → output/error TextAreas
    // =========================================================================
    private void bindRecord(ProcessRecord rec) {
        // Remove old listeners from previously bound record
        if (boundRecord != null) {
            if (outputListener != null) boundRecord
                .outputProperty()
                .removeListener(outputListener);
            if (errorListener != null) boundRecord
                .errorProperty()
                .removeListener(errorListener);
            if (statusListener != null) boundRecord
                .statusProperty()
                .removeListener(statusListener);
        }
        boundRecord = rec;

        if (rec == null) {
            outputArea.clear();
            errorArea.clear();
            if (statusLabel != null) statusLabel.setText("Ready");
            if (timeBadge != null) timeBadge.setText("—");
            return;
        }

        // Snapshot current text
        outputArea.setText(rec.getOutput());
        errorArea.setText(rec.getError());
        updateStatusBadge(rec.getStatus());
        if (timeBadge != null) {
            long ms = rec.getExecutionTimeMs();
            timeBadge.setText(
                ms > 0
                    ? String.format("%.3f s", ms / 1000.0)
                    : (rec.getStatus() == ExecutionStatus.RUNNING
                          ? "running…"
                          : "—")
            );
        }

        // Wire up live-update listeners
        outputListener = (obs, o, n) ->
            Platform.runLater(() -> {
                outputArea.setText(n);
                outputArea.setScrollTop(Double.MAX_VALUE);
            });
        errorListener = (obs, o, n) ->
            Platform.runLater(() -> {
                errorArea.setText(n);
                errorArea.setScrollTop(Double.MAX_VALUE);
            });
        statusListener = (obs, o, n) ->
            Platform.runLater(() -> updateStatusBadge(n));

        rec.outputProperty().addListener(outputListener);
        rec.errorProperty().addListener(errorListener);
        rec.statusProperty().addListener(statusListener);
        rec
            .executionTimeMsProperty()
            .addListener((obs, o, n) ->
                Platform.runLater(() -> {
                    if (timeBadge != null) timeBadge.setText(
                        String.format("%.3f s", n.longValue() / 1000.0)
                    );
                })
            );
    }

    private void updateStatusBadge(ExecutionStatus status) {
        if (statusLabel == null) return;
        statusLabel.setText(status.getLabel());
        try {
            statusLabel.setTextFill(Color.web(status.getColor()));
        } catch (Exception ignored) {}
        if (timeBadge != null && status == ExecutionStatus.RUNNING) {
            timeBadge.setText("running…");
        }
    }

    // =========================================================================
    //  Utilities
    // =========================================================================
    private void setStatus(String msg) {
        Platform.runLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(msg);
                statusLabel.setTextFill(Color.web("#cccccc"));
            }
        });
    }

    private Button styledBtn(String text, String styleClass) {
        Button b = new Button(text);
        b.getStyleClass().addAll("toolbar-btn", styleClass);
        return b;
    }

    private Tab closableTab(String title, Node content) {
        Tab t = new Tab(title, content);
        t.setClosable(false);
        return t;
    }

    public void shutdown() {
        bgPool.shutdownNow();
    }
}
