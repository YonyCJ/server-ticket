package com.prapp.pserver;

import com.prapp.pserver.printer.CorsOriginsDialog;
import com.prapp.pserver.printer.CorsOriginsStore;
import com.prapp.pserver.printer.TrayApp;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.nio.file.Files;

@SpringBootApplication
public class PserverApplication {

    public static void main(String[] args) {
        String def = "http://localhost:4200";

        if (!Files.exists(CorsOriginsStore.file())) {
            String picked = CorsOriginsDialog.ask(def);
            if (picked != null) {
                CorsOriginsStore.save(picked);
                TrayApp.enableStartup();
            } else {
                System.exit(0);
            }
        }
        System.setProperty("java.awt.headless", "false");
        new SpringApplicationBuilder(PserverApplication.class)
                .headless(false)
                .run(args);
    }
}