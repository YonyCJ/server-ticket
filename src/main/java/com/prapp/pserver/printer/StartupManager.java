package com.prapp.pserver.printer;

import java.io.File;
import java.io.IOException;

public class StartupManager {

    private static final String APP_NAME = "ServerTicketPServer";

    public static void enableStartup() {
        try {
            String appPath = System.getProperty("app.path");

            if (appPath == null || appPath.isBlank()) {
                File currentPath = new File(StartupManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                appPath = currentPath.getPath();

                if (appPath.endsWith(".jar")) {
                    String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "javaw.exe";
                    appPath = "\"" + javaBin + "\" -jar \"" + appPath + "\"";
                }
            } else {
                appPath = "\"" + appPath + "\"";
            }

            String command = "reg add \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run\" /v "
                    + APP_NAME + " /t REG_SZ /d " + appPath + " /f";

            Runtime.getRuntime().exec(command);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void disableStartup() {
        try {
            String command = "reg delete \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run\" /v "
                    + APP_NAME + " /f";
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}