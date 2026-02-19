package com.prapp.pserver.printer;

import lombok.Data;

import java.util.List;

@Data
public class PrintRequest {
    private String printerName;
    private List<Object> content;

    // Getters y setters
    public String getPrinterName() {
        return printerName;
    }

    public void setPrinterName(String printerName) {
        this.printerName = printerName;
    }

    public List<Object> getContent() {
        return content;
    }

    public void setContent(List<Object> content) {
        this.content = content;
    }
}
