package com.prapp.pserver.printer;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

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

        MenuItem origins = new MenuItem("CORS: " + System.getProperty("cors.allowed.origins", ""));
        origins.setEnabled(false);

        MenuItem edit = new MenuItem("Editar dominio CORS...");
        edit.addActionListener(e -> {
            String current = CorsOriginsStore.readOrDefault(System.getProperty("cors.allowed.origins", ""));
            String picked = CorsOriginsDialog.ask(current);
            if (picked != null) {
                CorsOriginsStore.save(picked);
                if (icon != null) {
                    icon.displayMessage("PServer", "Guardado. Reinicia para aplicar el nuevo dominio.", TrayIcon.MessageType.INFO);
                }
            }
        });

        MenuItem restart = new MenuItem("Reiniciar");
        restart.addActionListener(e -> restartSelf());

        MenuItem exit = new MenuItem("Salir");
        exit.addActionListener(e -> System.exit(0));

        menu.add(status);
        menu.add(origins);
        menu.addSeparator();
        menu.add(edit);
        menu.add(restart);
        menu.addSeparator();
        menu.add(exit);

        icon = new TrayIcon(img, "PServer", menu);
        icon.setImageAutoSize(true);

        tray.add(icon);

        icon.displayMessage("PServer", "Servidor escuchando en http://localhost:8095", TrayIcon.MessageType.INFO);
    }

    private void restartSelf() {
        try {
            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            String jarPath = new File(TrayApp.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();

            if (jarPath.endsWith(".jar")) {
                new ProcessBuilder(javaBin, "-jar", jarPath).start();
                System.exit(0);
                return;
            }

            icon.displayMessage("PServer", "No pude reiniciar automáticamente en este modo. Cierra y abre otra vez.", TrayIcon.MessageType.WARNING);
        } catch (Exception ex) {
            try {
                icon.displayMessage("PServer", "Error reiniciando. Cierra y abre otra vez.", TrayIcon.MessageType.ERROR);
            } catch (Exception ignored) {
            }
        }
    }
}
