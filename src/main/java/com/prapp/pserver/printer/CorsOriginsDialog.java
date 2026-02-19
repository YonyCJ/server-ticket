package com.prapp.pserver.printer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public final class CorsOriginsDialog {

    private CorsOriginsDialog() {}

    public static String ask(String currentValue) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        String preset = (currentValue == null || currentValue.isBlank())
                ? "https://gestionesavitac.top"
                : currentValue;

        JDialog dialog = new JDialog((Frame) null, "Server Ticket · Configuración CORS", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel title = new JLabel("Dominios permitidos");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JLabel help = new JLabel("<html>Escribe 1 o varios dominios (separados por coma).<br/>Ejemplo: <b>https://dominio.com</b>, <b>https://www.dominio.com</b></html>");
        help.setFont(help.getFont().deriveFont(12f));

        JTextField input = new JTextField(preset);
        input.setFont(input.getFont().deriveFont(13f));
        input.setColumns(38);

        JLabel error = new JLabel(" ");
        error.setForeground(new Color(180, 0, 0));
        error.setFont(error.getFont().deriveFont(12f));

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.add(help, BorderLayout.NORTH);
        center.add(input, BorderLayout.CENTER);
        center.add(error, BorderLayout.SOUTH);

        JButton btnCancel = new JButton("Cancelar");
        JButton btnSave = new JButton("Guardar");

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(btnCancel);
        actions.add(btnSave);

        root.add(title, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.pack();
        dialog.setLocationRelativeTo(null);

        final String[] result = new String[1];

        Runnable saveAction = () -> {
            String normalized = normalize(input.getText());
            List<String> bad = invalidOrigins(normalized);

            if (normalized.isBlank()) {
                error.setText("Ingresa al menos un dominio válido (https://...).");
                return;
            }
            if (!bad.isEmpty()) {
                error.setText("Dominio inválido: " + bad.get(0));
                return;
            }

            result[0] = normalized;
            dialog.dispose();
        };

        btnSave.addActionListener(e -> saveAction.run());
        btnCancel.addActionListener(e -> {
            result[0] = null;
            dialog.dispose();
        });

        input.addActionListener(e -> saveAction.run());

        dialog.setVisible(true);
        return result[0];
    }

    public static String normalize(String s) {
        if (s == null) return "";
        String v = s.trim();
        v = v.replace("\n", ",").replace("\r", ",").replace(" ", "");
        while (v.contains(",,")) v = v.replace(",,", ",");
        if (v.startsWith(",")) v = v.substring(1);
        if (v.endsWith(",")) v = v.substring(0, v.length() - 1);
        return v;
    }

    private static List<String> invalidOrigins(String normalized) {
        List<String> bad = new ArrayList<>();
        if (normalized == null || normalized.isBlank()) return bad;

        String[] parts = normalized.split(",");
        for (String p : parts) {
            String origin = p.trim();
            if (origin.isBlank()) continue;

            try {
                URI uri = URI.create(origin);
                String scheme = uri.getScheme();
                String host = uri.getHost();
                if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                    bad.add(origin);
                    continue;
                }
                if (host == null || host.isBlank()) {
                    bad.add(origin);
                }
            } catch (Exception ex) {
                bad.add(origin);
            }
        }
        return bad;
    }
}
