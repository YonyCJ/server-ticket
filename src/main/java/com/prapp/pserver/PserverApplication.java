package com.prapp.pserver;

import com.prapp.pserver.printer.CorsOriginsDialog;
import com.prapp.pserver.printer.CorsOriginsStore;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class PserverApplication {

    public static void main(String[] args) {
        String def = "https://dominio.com";
        String current = CorsOriginsStore.readOrDefault(def);

        String picked = CorsOriginsDialog.ask(current);
        if (picked != null) {
            CorsOriginsStore.save(picked);
            current = picked;
        }

        System.setProperty("cors.allowed.origins", current);

        new SpringApplicationBuilder(PserverApplication.class)
                .headless(false)
                .run(args);
    }
}
