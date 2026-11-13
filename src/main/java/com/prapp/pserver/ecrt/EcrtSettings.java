package com.prapp.pserver.ecrt;

public record EcrtSettings(
        String terminalHost,
        int terminalPort,
        int connectTimeoutMs,
        int saleTimeoutMs,
        int voidTimeoutMs,
        int closeTimeoutMs,
        String lineEnding,
        String accessToken
) {

    public boolean configurada() {
        return terminalHost != null && !terminalHost.isBlank() && terminalPort > 0 && terminalPort <= 65535;
    }

    public String terminador() {
        if ("lf".equalsIgnoreCase(lineEnding)) {
            return "\n";
        }
        if ("crlf".equalsIgnoreCase(lineEnding)) {
            return "\r\n";
        }
        return "";
    }
}
