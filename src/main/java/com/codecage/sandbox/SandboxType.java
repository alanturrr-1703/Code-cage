package com.codecage.sandbox;

public enum SandboxType {
    NONE  ("🔓 Unsafe", "#888888"),
    DOCKER("🔒 Docker", "#4ec9b0");

    private final String label;
    private final String color;

    SandboxType(String label, String color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() { return label; }
    public String getColor() { return color; }
}
