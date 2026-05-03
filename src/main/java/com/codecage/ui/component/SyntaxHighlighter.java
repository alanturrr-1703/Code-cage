package com.codecage.ui.component;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.*;
import java.util.regex.*;

public class SyntaxHighlighter {

    // --- Java ---
    private static final String[] JAVA_KW = {
        "abstract","assert","boolean","break","byte","case","catch","char","class",
        "const","continue","default","do","double","else","enum","extends","final",
        "finally","float","for","goto","if","implements","import","instanceof","int",
        "interface","long","native","new","package","private","protected","public",
        "return","short","static","strictfp","super","switch","synchronized","this",
        "throw","throws","transient","try","var","void","volatile","while","record","sealed","permits"
    };

    // --- Python ---
    private static final String[] PYTHON_KW = {
        "False","None","True","and","as","assert","async","await","break","class",
        "continue","def","del","elif","else","except","finally","for","from","global",
        "if","import","in","is","lambda","nonlocal","not","or","pass","raise","return",
        "try","while","with","yield"
    };

    // --- C++ ---
    private static final String[] CPP_KW = {
        "alignas","alignof","and","and_eq","asm","auto","bitand","bitor","bool","break",
        "case","catch","char","char8_t","char16_t","char32_t","class","compl","concept",
        "const","consteval","constexpr","constinit","const_cast","continue","co_await",
        "co_return","co_yield","decltype","default","delete","do","double","dynamic_cast",
        "else","enum","explicit","export","extern","false","float","for","friend","goto",
        "if","inline","int","long","mutable","namespace","new","noexcept","not","not_eq",
        "nullptr","operator","or","or_eq","private","protected","public","register",
        "reinterpret_cast","requires","return","short","signed","sizeof","static",
        "static_assert","static_cast","struct","switch","template","this","thread_local",
        "throw","true","try","typedef","typeid","typename","union","unsigned","using",
        "virtual","void","volatile","wchar_t","while","xor","xor_eq","include","define",
        "iostream","string","vector","cout","cin","endl"
    };

    // --- JavaScript ---
    private static final String[] JS_KW = {
        "abstract","arguments","await","boolean","break","byte","case","catch","char",
        "class","const","continue","debugger","default","delete","do","double","else",
        "enum","eval","export","extends","false","final","finally","float","for","function",
        "goto","if","implements","import","in","instanceof","int","interface","let","long",
        "native","new","null","package","private","protected","public","return","short",
        "static","super","switch","synchronized","this","throw","throws","transient","true",
        "try","typeof","undefined","var","void","volatile","while","with","yield","of","from","async","Arrow","=>","console","log"
    };

    // --- Bash ---
    private static final String[] BASH_KW = {
        "if","then","else","elif","fi","case","esac","for","select","while","until","do",
        "done","in","function","time","coproc","echo","exit","export","local","readonly",
        "return","set","shift","source","unset","eval","exec","cd","ls","mkdir","rm","cp",
        "mv","cat","grep","awk","sed","chmod","chown","sudo","apt","yum","brew"
    };

    private static Pattern buildPattern(String[] keywords, String commentRegex) {
        String kwPattern = "\\b(" + String.join("|", keywords) + ")\\b";
        return Pattern.compile(
                "(?<COMMENT>" + commentRegex + ")"
                + "|(?<STRING>(\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'))"
                + "|(?<NUMBER>\\b[0-9]+(\\.[0-9]+)?([eE][+-]?[0-9]+)?[fFdDlL]?\\b)"
                + "|(?<KEYWORD>" + kwPattern + ")"
                + "|(?<PAREN>[()])"
                + "|(?<BRACE>[{}])"
                + "|(?<BRACKET>\\[\\])",
                Pattern.DOTALL
        );
    }

    private static final Map<String, Pattern> PATTERNS = new HashMap<>();
    static {
        PATTERNS.put("java",       buildPattern(JAVA_KW,   "//[^\n]*|/\\*.*?\\*/"));
        PATTERNS.put("cpp",        buildPattern(CPP_KW,    "//[^\n]*|/\\*.*?\\*/|#[^\n]*"));
        PATTERNS.put("javascript", buildPattern(JS_KW,     "//[^\n]*|/\\*.*?\\*/"));
        PATTERNS.put("python",     buildPattern(PYTHON_KW, "#[^\n]*|\"\"\".*?\"\"\"|'''.*?'''"));
        PATTERNS.put("bash",       buildPattern(BASH_KW,   "#[^\n]*"));
    }

    public static StyleSpans<Collection<String>> computeHighlighting(String language, String text) {
        Pattern pattern = PATTERNS.getOrDefault(language.toLowerCase(), PATTERNS.get("python"));
        Matcher matcher = pattern.matcher(text);
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int lastEnd = 0;

        while (matcher.find()) {
            String styleClass = null;
            if (matcher.group("COMMENT") != null)       styleClass = "comment";
            else if (matcher.group("STRING")  != null)  styleClass = "string";
            else if (matcher.group("NUMBER")  != null)  styleClass = "number";
            else if (matcher.group("KEYWORD") != null)  styleClass = "keyword";
            else if (matcher.group("PAREN")   != null)  styleClass = "paren";
            else if (matcher.group("BRACE")   != null)  styleClass = "brace";
            else if (matcher.group("BRACKET") != null)  styleClass = "bracket";

            spansBuilder.add(Collections.emptyList(), matcher.start() - lastEnd);
            spansBuilder.add(styleClass != null
                    ? Collections.singleton(styleClass)
                    : Collections.emptyList(), matcher.end() - matcher.start());
            lastEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastEnd);
        return spansBuilder.create();
    }
}
