package com.prapp.pserver.ecrt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

@Component
public class EcrtTerminalClient {

    private static final int MAX_RESPUESTA_BYTES = 1024 * 1024;

    private final ObjectMapper mapper = new ObjectMapper();

    public boolean alcanzable(EcrtSettings settings) {
        if (!settings.configurada()) {
            return false;
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(settings.terminalHost(), settings.terminalPort()), settings.connectTimeoutMs());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public EcrtResult enviar(EcrtSettings settings, String comandoJson, int timeoutMs) {
        if (!settings.configurada()) {
            return EcrtResult.notSent("Falta configurar la IP y el puerto de la terminal en el agente.");
        }
        Socket socket = new Socket();
        try {
            try {
                socket.connect(new InetSocketAddress(settings.terminalHost(), settings.terminalPort()), settings.connectTimeoutMs());
            } catch (IOException e) {
                return EcrtResult.notSent("No se pudo conectar con la terminal. Verifique que esté encendida y en la red. No se cobró nada.");
            }

            try {
                OutputStream salida = socket.getOutputStream();
                salida.write((comandoJson + settings.terminador()).getBytes(StandardCharsets.UTF_8));
                salida.flush();
            } catch (IOException e) {
                return EcrtResult.noResponse("Se cortó la conexión mientras se enviaba el comando a la terminal.");
            }

            return leerRespuesta(socket, timeoutMs);
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private EcrtResult leerRespuesta(Socket socket, int timeoutMs) {
        long limite = System.currentTimeMillis() + timeoutMs;
        ByteArrayOutputStream acumulado = new ByteArrayOutputStream();
        byte[] bloque = new byte[4096];
        try {
            InputStream entrada = socket.getInputStream();
            while (true) {
                long restante = limite - System.currentTimeMillis();
                if (restante <= 0) {
                    return sinRespuestaCompleta(acumulado, "La terminal no respondió a tiempo.");
                }
                socket.setSoTimeout((int) Math.min(restante, Integer.MAX_VALUE));
                int leidos;
                try {
                    leidos = entrada.read(bloque);
                } catch (SocketTimeoutException e) {
                    return sinRespuestaCompleta(acumulado, "La terminal no respondió a tiempo.");
                }
                if (leidos < 0) {
                    String texto = texto(acumulado);
                    if (texto.isBlank()) {
                        return EcrtResult.noResponse("La terminal cerró la conexión sin responder.");
                    }
                    return EcrtResult.responded(texto);
                }
                acumulado.write(bloque, 0, leidos);
                if (acumulado.size() > MAX_RESPUESTA_BYTES) {
                    return EcrtResult.noResponse("La respuesta de la terminal es demasiado grande.");
                }
                String texto = texto(acumulado);
                if (esJsonCompleto(texto)) {
                    return EcrtResult.responded(texto);
                }
            }
        } catch (IOException e) {
            return sinRespuestaCompleta(acumulado, "Se cortó la conexión con la terminal antes de recibir la respuesta.");
        }
    }

    private EcrtResult sinRespuestaCompleta(ByteArrayOutputStream acumulado, String detalle) {
        String texto = texto(acumulado);
        if (esJsonCompleto(texto)) {
            return EcrtResult.responded(texto);
        }
        return EcrtResult.noResponse(texto.isBlank() ? detalle : detalle + " Llegó una respuesta incompleta.");
    }

    private boolean esJsonCompleto(String texto) {
        String limpio = texto.trim();
        if (limpio.isEmpty() || limpio.charAt(0) != '{' || limpio.charAt(limpio.length() - 1) != '}') {
            return false;
        }
        try {
            mapper.readTree(limpio);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String texto(ByteArrayOutputStream acumulado) {
        return acumulado.toString(StandardCharsets.UTF_8);
    }
}
