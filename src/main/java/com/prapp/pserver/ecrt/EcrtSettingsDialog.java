package com.prapp.pserver.ecrt;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public final class EcrtSettingsDialog {

    private EcrtSettingsDialog() {}

    public static EcrtSettings ask(EcrtSettings actual) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        JTextField host = new JTextField(actual.terminalHost(), 18);
        JTextField puerto = new JTextField(actual.terminalPort() > 0 ? String.valueOf(actual.terminalPort()) : "", 8);
        JTextField token = new JTextField(actual.accessToken(), 24);
        JButton regenerar = new JButton("Generar otra");
        regenerar.addActionListener(e -> token.setText(EcrtSettingsStore.nuevoToken()));

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        panel.add(new JLabel("IP de la terminal"), c);
        c.gridx = 1;
        panel.add(host, c);

        c.gridx = 0; c.gridy = 1;
        panel.add(new JLabel("Puerto de la terminal"), c);
        c.gridx = 1;
        panel.add(puerto, c);

        c.gridx = 0; c.gridy = 2;
        panel.add(new JLabel("Clave del agente"), c);
        c.gridx = 1;
        panel.add(token, c);
        c.gridx = 2;
        panel.add(regenerar, c);

        c.gridx = 0; c.gridy = 3; c.gridwidth = 3;
        panel.add(new JLabel("<html>La IP y el puerto salen en la terminal, en <b>Datos de conexión</b>.<br/>Copie la clave en el POS: botón <b>Tarjeta</b>, campo <b>Clave del agente</b>.</html>"), c);

        while (true) {
            int opcion = JOptionPane.showConfirmDialog(null, panel, "Server Ticket · Terminal de tarjeta",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (opcion != JOptionPane.OK_OPTION) {
                return null;
            }
            String hostValor = host.getText().trim();
            String tokenValor = token.getText().trim();
            int puertoValor;
            try {
                puertoValor = Integer.parseInt(puerto.getText().trim());
            } catch (NumberFormatException e) {
                puertoValor = 0;
            }
            if (hostValor.isEmpty() || puertoValor < 1 || puertoValor > 65535) {
                JOptionPane.showMessageDialog(null, "Indique la IP y un puerto entre 1 y 65535.", "Datos incompletos", JOptionPane.WARNING_MESSAGE);
                continue;
            }
            if (tokenValor.length() < 8) {
                JOptionPane.showMessageDialog(null, "La clave del agente debe tener al menos 8 caracteres.", "Clave muy corta", JOptionPane.WARNING_MESSAGE);
                continue;
            }
            return new EcrtSettings(hostValor, puertoValor, actual.connectTimeoutMs(), actual.saleTimeoutMs(),
                    actual.voidTimeoutMs(), actual.closeTimeoutMs(), actual.lineEnding(), tokenValor);
        }
    }
}
