# Code-cage ⚙️

A **multiprocess code execution platform** built with JavaFX 21. Write, run, and monitor code in five languages simultaneously — each execution is an isolated OS process with real-time stdout/stderr streaming, time limits, memory limits, and live status tracking.

![Java](https://img.shields.io/badge/Java-21-blue?logo=openjdk)
![JavaFX](https://img.shields.io/badge/JavaFX-21-blueviolet?logo=java)
![Gradle](https://img.shields.io/badge/Gradle-9.4-02303A?logo=gradle)
![License](https://img.shields.io/badge/License-MIT-green)

---

## Features

- **5 languages** — Java, Python 3, C++17, JavaScript (Node.js), Bash
- **Multiprocess** — unlimited concurrent runs, each in its own OS process
- **Real-time streaming** — stdout and stderr stream line-by-line into the UI as the process runs
- **Time & memory limits** — configurable per run; TLE kills the process automatically
- **Process history sidebar** — live status badges (PENDING / RUNNING / SUCCESS / TLE / RE / CE / KILLED) with execution time
- **Kill on demand** — stop any running process instantly
- **Syntax highlighting** — regex-based token highlighting for all 5 languages via RichTextFX
- **Multi-tab editor** — open multiple files simultaneously, each with line numbers
- **Chaos Monkey ready** — `ProcessManager` exposes a live `ObservableList<ProcessRecord>` so a Chaos Monkey can randomly kill running processes to test platform resilience
- **VS Code dark theme** — full CSS dark theme

---

## Architecture

```
com.codecage/
├── model/
│   ├── ExecutionStatus     # PENDING | RUNNING | SUCCESS | TLE | RE | CE | KILLED
│   ├── ExecutionResult     # immutable result value-object
│   ├── RunRequest          # code + language + timeLimitMs + memoryLimitMb + stdin (Builder)
│   ├── RunResponse         # simplified API wrapper (OOD compatibility)
│   ├── Language            # enum: JAVA | PYTHON | CPP | JAVASCRIPT | BASH
│   └── ProcessRecord       # JavaFX-observable live row (properties + kill())
│
├── executor/
│   ├── CodeExecutor        # interface: execute(RunRequest) + streaming overload
│   ├── BaseExecutor        # template: temp-dir, async I/O threads, TLE watchdog
│   ├── JavaExecutor        # javac compile → java -Xmx run
│   ├── PythonExecutor      # python3/python auto-detection, -u unbuffered
│   ├── CppExecutor         # g++ -O2 -std=c++17 compile → native binary
│   ├── JavaScriptExecutor  # node runtime
│   ├── BashExecutor        # bash interpreter
│   └── ExecutorFactory     # static registry: getExecutor(languageId)
│
├── service/
│   ├── ProcessManager      # singleton ObservableList<ProcessRecord> + kill/clear ops
│   └── ExecutionService    # JavaFX Service<ExecutionResult> wrapping a background Task
│
└── ui/
    ├── MainApp             # Application bootstrap, CSS load, stage sizing
    ├── controller/
    │   └── MainController  # full programmatic scene graph, toolbar, split panes
    └── component/
        ├── ProcessListCell # custom ListCell with live-updating status badge
        └── SyntaxHighlighter # StyleSpansBuilder regex highlighter (DOTALL)
```

### Threading model

| Layer | Thread |
|---|---|
| UI updates | JavaFX Application Thread |
| Code execution task | `CachedThreadPool` daemon thread |
| stdout reader | daemon thread per process |
| stderr reader | daemon thread per process |

Every process spawns **3 JVM threads**. With N concurrent runs you have `3N` background threads + `N` child OS processes + 1 JVM.

---

## Execution lifecycle

```
Run clicked
    └─ RunRequest built (code + language + limits + stdin)
    └─ ProcessRecord created → added to ProcessManager (status: PENDING)
    └─ ExecutionService.start()
           └─ Task runs on bgPool
                  └─ status → RUNNING
                  └─ ExecutorFactory.getExecutor(lang).execute(request, outCb, errCb)
                         └─ BaseExecutor: creates temp dir
                         └─ [compile if needed]  ──→ CE on failure
                         └─ ProcessBuilder.start()
                         └─ stdin fed, stdout/stderr read on daemon threads
                         └─ waitFor(timeLimitMs)  ──→ TLE + destroyForcibly()
                         └─ exit code 0 → SUCCESS | non-zero → RE
                  └─ ProcessRecord.applyResult() on FX thread
                  └─ temp dir deleted
```

---

## Requirements

| Tool | Minimum version |
|---|---|
| JDK | 21 |
| Gradle | 9 (wrapper included) |
| Python | 3.x (for Python runs) |
| g++ | any modern (for C++ runs) |
| Node.js | any LTS (for JS runs) |
| Bash | any (for Bash runs) |

Only JDK 21 is required to build and run the platform itself. Language runtimes are optional — the platform will show a Runtime Error if a runtime is not found.

---

## Getting started

```bash
git clone https://github.com/alanturrr-1703/Code-cage.git
cd Code-cage
./gradlew run
```

That's it. Gradle downloads all dependencies (JavaFX 21, RichTextFX 0.11.2) automatically.

---

## Usage

1. **New Tab** — opens a fresh editor tab pre-filled with the selected language's template
2. **Language selector** — switch between Java / Python / C++ / JavaScript / Bash
3. **⏱ Time limit** — max wall-clock milliseconds before the process is killed (TLE)
4. **💾 Memory limit** — `-Xmx` cap for Java; advisory for other runtimes
5. **▶ Run** — compiles (if needed) and runs the current editor tab as a new process
6. **■ Kill** — kills the selected process in the history sidebar
7. **⏹ Stop All** — kills every running process
8. **🗑 Clear** — removes all completed/failed entries from the sidebar
9. **Process sidebar** — click any record to bind its stdout/stderr/status to the output panels
10. **Input panel** — text typed here is fed as stdin to the next run

---

## Sandbox mode (Docker)

By default Code-cage runs user code natively — it has process isolation (each run is a separate OS process) but **no filesystem, network, or fork-bomb protection**. Toggle **🔒 Docker** in the toolbar to enable full sandboxing.

### What gets locked down

| Threat | Mitigation |
|---|---|
| Network access | `--network none` — all TCP/UDP blocked at the kernel level |
| Host filesystem writes | `--read-only` root FS + bind-mount source as `:ro` |
| Writes outside /tmp | Only `/tmp` is writable (tmpfs, 128 MB cap, auto-deleted on exit) |
| Fork bombs | `--pids-limit 50` — container is killed if process count exceeds 50 |
| Memory exhaustion | `--memory` + `--memory-swap` set to the configured limit, no swap |
| CPU exhaustion | `--cpus 1.0` — hard cap at one logical core |

### Docker images used

| Language | Image |
|---|---|
| Java | `openjdk:21-slim` |
| Python | `python:3.12-slim` |
| C++ | `gcc:13` |
| JavaScript | `node:20-slim` |
| Bash | `bash:5.2` |

### How it works

```
Toggle ON
    └─ DockerAvailability checks docker info (cached at startup)
    └─ DockerImagePuller.pullAllAsync() pulls all 5 images in background
           └─ skips images already in local cache
           └─ status bar shows pull progress

Run clicked (sandbox ON)
    └─ RunRequest.sandboxed = true
    └─ ExecutorFactory returns DockerSandboxExecutor (instead of native)
    └─ DockerSandboxExecutor:
           └─ writes code file to workDir
           └─ builds: docker run --rm -i --network none --read-only
                       --tmpfs /tmp:rw,exec,size=128m
                       --memory {limit}m --memory-swap {limit}m
                       --cpus 1.0 --pids-limit 50
                       -w /tmp -v <workDir>:/sandbox:ro
                       <image> <language-command>
           └─ stdout/stderr streamed back through existing callbacks
           └─ TLE watchdog still enforced by BaseExecutor.runProcess()
```

### First-run note

Images are pulled automatically when sandbox is first enabled. Total download is ~700 MB (one-time). Subsequent runs are instant — Docker uses the local image cache.

---

## Chaos Monkey compatibility

Code-cage is structurally compatible with Chaos Monkey testing. Because each run is a fully isolated OS process tracked in a shared `ObservableList<ProcessRecord>`, a Chaos Monkey service can:

```java
// Filter live targets
List<ProcessRecord> running = ProcessManager.getInstance()
    .getRecords().stream()
    .filter(r -> r.getStatus() == ExecutionStatus.RUNNING)
    .toList();

// Pick a random victim and kill it
if (!running.isEmpty()) {
    ProcessRecord victim = running.get(random.nextInt(running.size()));
    victim.kill();   // calls Process.destroyForcibly() under the hood
}
```

The UI responds instantly — the status badge turns **KILLED**, the output panel updates, and the process is marked in the sidebar. Use long-running programs (e.g., infinite loops with a high time limit) to keep processes alive long enough for Chaos Monkey to strike.

---

## Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| JavaFX | 21 | UI framework |
| RichTextFX | 0.11.2 | Syntax-highlighted code editor |
| Flowless | 0.7.0 | Virtual flow (RichTextFX dep) |
| ReactFX | 2.0-M5 | Event stream debouncing (highlighting) |

---

## License

MIT — do whatever you want with it.
