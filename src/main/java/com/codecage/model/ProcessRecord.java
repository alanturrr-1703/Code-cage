package com.codecage.model;

import javafx.application.Platform;
import javafx.beans.property.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;

public class ProcessRecord {
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final int id;
    private final StringProperty language         = new SimpleStringProperty();
    private final StringProperty codePreview      = new SimpleStringProperty();
    private final ObjectProperty<ExecutionStatus> status =
            new SimpleObjectProperty<>(ExecutionStatus.PENDING);
    private final StringProperty output           = new SimpleStringProperty("");
    private final StringProperty error            = new SimpleStringProperty("");
    private final LongProperty executionTimeMs    = new SimpleLongProperty(0);
    private final IntegerProperty exitCode        = new SimpleIntegerProperty(-1);
    private final StringProperty startTime        = new SimpleStringProperty();

    // volatile process handle for killing
    private final AtomicReference<Process> processRef = new AtomicReference<>();

    public ProcessRecord(int id, String language, String code) {
        this.id = id;
        this.language.set(language);
        // Show first non-empty line as preview
        String preview = code.lines()
                .filter(l -> !l.isBlank())
                .findFirst()
                .orElse("(empty)")
                .trim();
        this.codePreview.set(preview.length() > 50 ? preview.substring(0, 47) + "..." : preview);
        this.startTime.set(LocalDateTime.now().format(FMT));
    }

    /** Append a chunk of text to the live output (thread-safe). */
    public void appendOutput(String chunk) {
        if (Platform.isFxApplicationThread()) {
            output.set(output.get() + chunk);
        } else {
            Platform.runLater(() -> output.set(output.get() + chunk));
        }
    }

    /** Append a chunk of text to the live error (thread-safe). */
    public void appendError(String chunk) {
        if (Platform.isFxApplicationThread()) {
            error.set(error.get() + chunk);
        } else {
            Platform.runLater(() -> error.set(error.get() + chunk));
        }
    }

    public void applyResult(ExecutionResult result) {
        Platform.runLater(() -> {
            if (!result.getOutput().isEmpty()) output.set(result.getOutput());
            if (!result.getError().isEmpty())  error.set(result.getError());
            status.set(result.getStatus());
            executionTimeMs.set(result.getExecutionTimeMs());
            exitCode.set(result.getExitCode());
        });
    }

    public void kill() {
        Process p = processRef.get();
        if (p != null && p.isAlive()) {
            p.destroyForcibly();
            Platform.runLater(() -> status.set(ExecutionStatus.KILLED));
        }
    }

    public void setProcess(Process p) {
        processRef.set(p);
    }

    // --- Getters ---
    public int                              getId()                  { return id; }
    public StringProperty                   languageProperty()       { return language; }
    public StringProperty                   codePreviewProperty()    { return codePreview; }
    public ObjectProperty<ExecutionStatus>  statusProperty()         { return status; }
    public StringProperty                   outputProperty()         { return output; }
    public StringProperty                   errorProperty()          { return error; }
    public LongProperty                     executionTimeMsProperty(){ return executionTimeMs; }
    public IntegerProperty                  exitCodeProperty()       { return exitCode; }
    public StringProperty                   startTimeProperty()      { return startTime; }

    public String           getLanguage()      { return language.get(); }
    public String           getCodePreview()   { return codePreview.get(); }
    public ExecutionStatus  getStatus()        { return status.get(); }
    public String           getOutput()        { return output.get(); }
    public String           getError()         { return error.get(); }
    public long             getExecutionTimeMs(){ return executionTimeMs.get(); }
    public int              getExitCode()      { return exitCode.get(); }
    public String           getStartTime()     { return startTime.get(); }

    public void setStatus(ExecutionStatus s) {
        Platform.runLater(() -> status.set(s));
    }
}
