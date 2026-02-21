package com.prapp.pserver.printer;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

@Component
public class TrayApp {

    private TrayIcon icon;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() throws Exception {
        if (!SystemTray.isSupported()) return;

        SystemTray tray = SystemTray.getSystemTray();
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(44, 90, 160));
        g.fillRoundRect(1, 1, 14, 14, 4, 4);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.drawString("P", 4, 12);
        g.dispose();

        PopupMenu menu = new PopupMenu();

        MenuItem status = new MenuItem("Servidor: http://localhost:8095");
        status.setEnabled(false);

        MenuItem edit = new MenuItem("Editar dominio CORS...");
        edit.addActionListener(e -> {
            String current = CorsOriginsStore.readOrDefault("http://localhost:4200");
            String picked = CorsOriginsDialog.ask(current);
            if (picked != null) {
                CorsOriginsStore.save(picked);
                if (icon != null) {
                    icon.displayMessage("PServer", "Configuración actualizada.", TrayIcon.MessageType.INFO);
                }
            }
        });

        CheckboxMenuItem startupItem = new CheckboxMenuItem("Arrancar al encender PC", isStartupEnabled());
        startupItem.addItemListener(e -> {
            if (startupItem.getState()) {
                enableStartup();
            } else {
                disableStartup();
            }
        });

        MenuItem restart = new MenuItem("Reiniciar");
        restart.addActionListener(e -> restartSelf());

        MenuItem exit = new MenuItem("Salir");
        exit.addActionListener(e -> System.exit(0));

        menu.add(status);
        menu.addSeparator();
        menu.add(edit);
        menu.add(startupItem);
        menu.add(restart);
        menu.addSeparator();
        menu.add(exit);

        icon = new TrayIcon(img, "PServer", menu);
        icon.setImageAutoSize(true);
        tray.add(icon);
    }

    public static boolean isStartupEnabled() {
        try {
            Process process = Runtime.getRuntime().exec("reg query \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run\" /v PServer");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return reader.lines().anyMatch(line -> line.contains("PServer"));
        } catch (Exception e) {
            return false;
        }
    }

    public static void enableStartup() {
        try {
            String appPath = System.getProperty("app.path");
            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "javaw.exe";

            if (appPath == null || appPath.isBlank()) {
                File currentPath = new File(TrayApp.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                String path = currentPath.getPath();

                if (path.endsWith(".jar")) {
                    appPath = "\"" + javaBin + "\" -jar \"" + path + "\"";
                } else {
                    appPath = "\"" + javaBin + "\" -cp \"" + path + "\" com.prapp.pserver.PserverApplication";
                }
            } else {
                appPath = "\"" + appPath + "\"";
            }
            if (appPath.length() > 2000) {
                System.out.println("Path demasiado largo para el registro (Modo IDE). Saltando registro real.");
                return;
            }

            String[] cmd = {
                    "reg", "add", "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                    "/v", "PServer", "/t", "REG_SZ", "/d", appPath, "/f"
            };

            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor();

            System.out.println("Comando registro ejecutado.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void disableStartup() {
        try {
            Runtime.getRuntime().exec("reg delete \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run\" /v PServer /f");
        } catch (Exception ignored) {}
    }

    private void restartSelf() {
        try {
            String appPath = System.getProperty("app.path");
            if (appPath != null && !appPath.isBlank()) {
                new ProcessBuilder(appPath).start();
                System.exit(0);
                return;
            }
            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            File currentPath = new File(TrayApp.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            String path = currentPath.getPath();
            ProcessBuilder builder = path.endsWith(".jar")
                    ? new ProcessBuilder(javaBin, "-jar", path)
                    : new ProcessBuilder(javaBin, "-cp", System.getProperty("java.class.path"), "com.prapp.pserver.PserverApplication");
            builder.start();
            System.exit(0);
        } catch (Exception ex) {
            if (icon != null) icon.displayMessage("PServer", "Error: " + ex.getMessage(), TrayIcon.MessageType.ERROR);
        }
    }
}