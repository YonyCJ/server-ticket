package com.prapp.pserver.ecrt;

import org.springframework.stereotype.Component;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Properties;

@Component
public class EcrtSettingsStore {

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int LARGO_TOKEN = 24;

    public static Path file() {
        return Path.of(System.getProperty("user.home"), ".pserver", "ecrt.properties");
    }

    public synchronized EcrtSettings load() {
        Properties props = new Properties();
        Path archivo = file();
        if (Files.exists(archivo)) {
            try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
                props.load(lector);
            } catch (Exception e) {
                System.err.println("ECRT - No se pudo leer " + archivo + ": " + e.getMessage());
            }
        }
        String token = texto(props, "accessToken", "");
        EcrtSettings settings = new EcrtSettings(
                texto(props, "terminalHost", ""),
                entero(props, "terminalPort", 0),
                entero(props, "connectTimeoutMs", 5000),
                entero(props, "saleTimeoutMs", 120000),
                entero(props, "voidTimeoutMs", 60000),
                entero(props, "closeTimeoutMs", 180000),
                texto(props, "lineEnding", "none"),
                token.isBlank() ? nuevoToken() : token
        );
        if (token.isBlank()) {
            save(settings);
        }
        return settings;
    }

    public synchronized void save(EcrtSettings settings) {
        Properties props = new Properties();
        props.setProperty("terminalHost", settings.terminalHost() == null ? "" : settings.terminalHost().trim());
        props.setProperty("terminalPort", String.valueOf(settings.terminalPort()));
        props.setProperty("connectTimeoutMs", String.valueOf(settings.connectTimeoutMs()));
        props.setProperty("saleTimeoutMs", String.valueOf(settings.saleTimeoutMs()));
        props.setProperty("voidTimeoutMs", String.valueOf(settings.voidTimeoutMs()));
        props.setProperty("closeTimeoutMs", String.valueOf(settings.closeTimeoutMs()));
        props.setProperty("lineEnding", settings.lineEnding() == null ? "none" : settings.lineEnding());
        props.setProperty("accessToken", settings.accessToken() == null ? "" : settings.accessToken().trim());
        try {
            Path archivo = file();
            Files.createDirectories(archivo.getParent());
            try (Writer escritor = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
                props.store(escritor, "Terminal de tarjeta (ECRT)");
            }
        } catch (Exception e) {
            System.err.println("ECRT - No se pudo guardar la configuracion: " + e.getMessage());
        }
    }

    public static String nuevoToken() {
        SecureRandom azar = new SecureRandom();
        StringBuilder token = new StringBuilder(LARGO_TOKEN);
        for (int i = 0; i < LARGO_TOKEN; i++) {
            token.append(ALFABETO.charAt(azar.nextInt(ALFABETO.length())));
        }
        return token.toString();
    }

    private static String texto(Properties props, String clave, String porDefecto) {
        String valor = props.getProperty(clave);
        return valor == null || valor.isBlank() ? porDefecto : valor.trim();
    }

    private static int entero(Properties props, String clave, int porDefecto) {
        try {
            int valor = Integer.parseInt(props.getProperty(clave, "").trim());
            return valor > 0 ? valor : porDefecto;
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }
}
