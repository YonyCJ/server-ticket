package com.prapp.pserver.ecrt;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record EcrtResult(Status status, String raw, String detail) {

    public enum Status {
        RESPONDED,
        NOT_SENT,
        NO_RESPONSE,
        BUSY,
        INVALID_REQUEST
    }

    public static EcrtResult responded(String raw) {
        return new EcrtResult(Status.RESPONDED, raw, null);
    }

    public static EcrtResult notSent(String detail) {
        return new EcrtResult(Status.NOT_SENT, null, detail);
    }

    public static EcrtResult noResponse(String detail) {
        return new EcrtResult(Status.NO_RESPONSE, null, detail);
    }

    public static EcrtResult busy() {
        return new EcrtResult(Status.BUSY, null, "La terminal está ocupada con otro cobro. Espere y reintente.");
    }

    public static EcrtResult invalid(String detail) {
        return new EcrtResult(Status.INVALID_REQUEST, null, detail);
    }
}
