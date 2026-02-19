package com.prapp.pserver.printer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CorsOriginsStore {

    private CorsOriginsStore() {}

    public static Path file() {
        String home = System.getProperty("user.home");
        return Path.of(home, ".pserver", "cors-origins.txt");
    }

    public static String readOrDefault(String def) {
        try {
            Path f = file();
            if (!Files.exists(f)) return def;
            String v = Files.readString(f, StandardCharsets.UTF_8).trim();
            return v.isBlank() ? def : v;
        } catch (Exception e) {
            return def;
        }
    }

    public static void save(String value) {
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            Files.writeString(f, value, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }
}
