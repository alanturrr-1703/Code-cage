package com.codecage.service;

import com.codecage.model.ProcessRecord;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.concurrent.atomic.AtomicInteger;

public class ProcessManager {
    private static final ProcessManager INSTANCE = new ProcessManager();
    private final ObservableList<ProcessRecord> records =
            FXCollections.observableArrayList();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    private ProcessManager() {}

    public static ProcessManager getInstance() { return INSTANCE; }

    public ProcessRecord createRecord(String language, String code) {
        ProcessRecord rec = new ProcessRecord(idCounter.getAndIncrement(), language, code);
        records.add(rec);
        return rec;
    }

    public void killAll() {
        records.forEach(ProcessRecord::kill);
    }

    public void clearCompleted() {
        records.removeIf(r -> {
            var s = r.getStatus();
            return s != com.codecage.model.ExecutionStatus.RUNNING
                && s != com.codecage.model.ExecutionStatus.PENDING;
        });
    }

    public ObservableList<ProcessRecord> getRecords() { return records; }
}
