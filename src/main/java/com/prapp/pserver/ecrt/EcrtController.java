package com.prapp.pserver.ecrt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prapp.pserver.printer.CorsOriginsStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@RestController
@RequestMapping("/ecrt")
public class EcrtController {

    private final EcrtSettingsStore settingsStore;
    private final EcrtTerminalClient terminal;
    private final CorsOriginsStore originsStore;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ReentrantLock enCurso = new ReentrantLock();

    public EcrtController(EcrtSettingsStore settingsStore, EcrtTerminalClient terminal, CorsOriginsStore originsStore) {
        this.settingsStore = settingsStore;
        this.terminal = terminal;
        this.originsStore = originsStore;
    }

    @GetMapping("/health")
    public ResponseEntity<?> health(HttpServletRequest request, @RequestParam(name = "probar", defaultValue = "false") boolean probar) {
        EcrtSettings settings = settingsStore.load();
        ResponseEntity<?> rechazo = rechazar(request, settings);
        if (rechazo != null) {
            return rechazo;
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("ok", true);
        cuerpo.put("configured", settings.configurada());
        cuerpo.put("terminalHost", settings.terminalHost());
        cuerpo.put("terminalPort", settings.terminalPort());
        cuerpo.put("busy", enCurso.isLocked());
        if (!settings.configurada()) {
            cuerpo.put("reachable", false);
            cuerpo.put("detail", "Falta configurar la IP y el puerto de la terminal en el agente.");
        } else if (probar && !enCurso.isLocked()) {
            cuerpo.put("reachable", terminal.alcanzable(settings));
        }
        return ResponseEntity.ok(cuerpo);
    }

    @PostMapping("/sale")
    public ResponseEntity<?> sale(HttpServletRequest request, @RequestBody(required = false) JsonNode body) {
        EcrtSettings settings = settingsStore.load();
        ResponseEntity<?> rechazo = rechazar(request, settings);
        if (rechazo != null) {
            return rechazo;
        }
        Long base = entero(body, "baseAmount");
        Long tax1 = enteroOCero(body, "tax1Amount");
        Long tax2 = enteroOCero(body, "tax2Amount");
        Long tip = enteroOCero(body, "tipAmount");
        String tokenEcr = texto(body, "tokenECR");
        if (base == null || base < 1 || tax1 == null || tax2 == null || tip == null || tokenEcr == null) {
            return ResponseEntity.ok(EcrtResult.invalid("Faltan baseAmount o tokenECR, o hay montos inválidos."));
        }
        ObjectNode comando = mapper.createObjectNode();
        comando.put("command", "SALE");
        comando.put("baseAmount", base);
        comando.put("tax1Amount", tax1);
        comando.put("tax2Amount", tax2);
        comando.put("tipAmount", tip);
        comando.put("tokenECR", tokenEcr);
        return ejecutar("SALE", tokenEcr, comando, settings, settings.saleTimeoutMs());
    }

    @PostMapping("/void")
    public ResponseEntity<?> anular(HttpServletRequest request, @RequestBody(required = false) JsonNode body) {
        EcrtSettings settings = settingsStore.load();
        ResponseEntity<?> rechazo = rechazar(request, settings);
        if (rechazo != null) {
            return rechazo;
        }
        String tokenId = texto(body, "tokenId");
        if (tokenId == null) {
            return ResponseEntity.ok(EcrtResult.invalid("Falta tokenId."));
        }
        ObjectNode comando = mapper.createObjectNode();
        comando.put("command", "VOID");
        comando.put("tokenId", tokenId);
        return ejecutar("VOID", tokenId, comando, settings, settings.voidTimeoutMs());
    }

    @PostMapping("/close")
    public ResponseEntity<?> cerrar(HttpServletRequest request) {
        EcrtSettings settings = settingsStore.load();
        ResponseEntity<?> rechazo = rechazar(request, settings);
        if (rechazo != null) {
            return rechazo;
        }
        ObjectNode comando = mapper.createObjectNode();
        comando.put("command", "CLOSE");
        return ejecutar("CLOSE", "-", comando, settings, settings.closeTimeoutMs());
    }

    private ResponseEntity<?> ejecutar(String nombre, String referencia, ObjectNode comando, EcrtSettings settings, int timeoutMs) {
        if (!enCurso.tryLock()) {
            System.out.println("ECRT " + nombre + " ref=" + referencia + " -> BUSY");
            return ResponseEntity.ok(EcrtResult.busy());
        }
        long inicio = System.currentTimeMillis();
        try {
            EcrtResult resultado = terminal.enviar(settings, comando.toString(), timeoutMs);
            System.out.println("ECRT " + nombre + " ref=" + referencia + " -> " + resultado.status()
                    + " en " + (System.currentTimeMillis() - inicio) + " ms");
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            System.err.println("ECRT " + nombre + " ref=" + referencia + " -> error inesperado: " + e.getClass().getSimpleName());
            return ResponseEntity.ok(EcrtResult.noResponse("Fallo inesperado del agente durante el comando."));
        } finally {
            enCurso.unlock();
        }
    }

    private ResponseEntity<?> rechazar(HttpServletRequest request, EcrtSettings settings) {
        if (!esLocal(request.getRemoteAddr())) {
            return error(HttpStatus.FORBIDDEN, "El agente de la terminal solo atiende a esta misma PC.");
        }
        String origen = request.getHeader("Origin");
        if (origen != null && !origen.isBlank() && !originsStore.getOrigins().contains(origen)) {
            return error(HttpStatus.FORBIDDEN, "Origen no permitido.");
        }
        String token = request.getHeader("X-Agent-Token");
        if (token == null || !iguales(token.trim(), settings.accessToken())) {
            return error(HttpStatus.UNAUTHORIZED, "Clave del agente ausente o incorrecta.");
        }
        return null;
    }

    private ResponseEntity<?> error(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("error", mensaje));
    }

    private boolean esLocal(String direccion) {
        try {
            return InetAddress.getByName(direccion).isLoopbackAddress();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean iguales(String a, String b) {
        if (b == null || b.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private Long entero(JsonNode body, String campo) {
        if (body == null) {
            return null;
        }
        JsonNode nodo = body.get(campo);
        if (nodo == null || !nodo.isIntegralNumber() || !nodo.canConvertToLong()) {
            return null;
        }
        return nodo.longValue();
    }

    private Long enteroOCero(JsonNode body, String campo) {
        if (body == null) {
            return null;
        }
        JsonNode nodo = body.get(campo);
        if (nodo == null || nodo.isNull()) {
            return 0L;
        }
        Long valor = entero(body, campo);
        return valor == null || valor < 0 ? null : valor;
    }

    private String texto(JsonNode body, String campo) {
        if (body == null) {
            return null;
        }
        JsonNode nodo = body.get(campo);
        if (nodo == null || !nodo.isTextual() || nodo.asText().isBlank()) {
            return null;
        }
        return nodo.asText().trim();
    }
}
