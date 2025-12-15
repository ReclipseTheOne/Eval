package com.reclipse.eval;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class EvalConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Path.of("config", "eval.json");
    private static EvalConfig INSTANCE;

    public boolean inlineEnabled = true;
    public String inlinePatternStart = "{{";
    public String inlinePatternEnd = "}}";
    public Map<String, Double> variables = new HashMap<>();

    public static EvalConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                INSTANCE = GSON.fromJson(json, EvalConfig.class);
            } else {
                INSTANCE = new EvalConfig();
                save();
            }
        } catch (IOException e) {
            Constants.LOG.error("Failed to load config", e);
            INSTANCE = new EvalConfig();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            Constants.LOG.error("Failed to save config", e);
        }
    }
}
