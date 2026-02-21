package com.prapp.pserver.printer;

import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CorsOriginsStore {

    public CorsOriginsStore() {}

    public List<String> getOrigins() {
        return parseOrigins(readOrDefault("http://localhost:4200"));
    }

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
        } catch (Exception ignored) {}
    }

    private List<String> parseOrigins(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }
}