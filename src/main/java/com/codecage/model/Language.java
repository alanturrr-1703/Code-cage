package com.codecage.model;

public enum Language {
    JAVA("Java",       "java",       "java",       "// Java code here\npublic class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, World!\");\n    }\n}"),
    PYTHON("Python 3", "python",     "py",         "# Python code here\nprint('Hello, World!')"),
    CPP("C++",         "cpp",        "cpp",        "// C++ code here\n#include <iostream>\nusing namespace std;\nint main() {\n    cout << \"Hello, World!\" << endl;\n    return 0;\n}"),
    JAVASCRIPT("JavaScript","javascript","js",      "// JavaScript code here\nconsole.log('Hello, World!');"),
    BASH("Bash",       "bash",       "sh",         "#!/bin/bash\n# Bash script here\necho 'Hello, World!'");

    private final String displayName;
    private final String id;
    private final String extension;
    private final String template;

    Language(String displayName, String id, String extension, String template) {
        this.displayName = displayName;
        this.id = id;
        this.extension = extension;
        this.template = template;
    }

    public String getDisplayName() { return displayName; }
    public String getId()          { return id; }
    public String getExtension()   { return extension; }
    public String getTemplate()    { return template; }

    public static Language fromId(String id) {
        for (Language l : values()) {
            if (l.id.equals(id)) return l;
        }
        return PYTHON;
    }

    @Override
    public String toString() { return displayName; }
}
