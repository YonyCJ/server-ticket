package com.prapp.pserver.printer;

import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.springframework.boot.SpringApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.PrinterName;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

@RestController
@RequestMapping("/print")
public class PrinterServer {
    public static void main(String[] args) {
        SpringApplication.run(PrinterServer.class, args);
    }


    @PostMapping
    public ResponseEntity<Map<String, String>> print(@RequestBody PrintRequest request) {
        Map<String, String> response = new HashMap<>();
        try {
            sendToPrinter(request);
            response.put("message", "Impresión enviada con éxito a " + request.getPrinterName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Error al imprimir en servidor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    private void sendToPrinter(PrintRequest request) throws Exception {
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        PrintService selectedPrinter = null;

        for (PrintService service : printServices) {
            if (service.getName().equalsIgnoreCase(request.getPrinterName())) {
                selectedPrinter = service;
                break;
            }
        }

        if (selectedPrinter == null) {
            throw new Exception("Impresora no encontrada: " + request.getPrinterName());
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Comandos ESC/POS
        outputStream.write(new byte[]{0x1B, 0x40}); // Reset

        // Procesar el contenido dinámico
        for (Object item : request.getContent()) {
            if (item instanceof String) {
                // Si es texto simple
                outputStream.write(new byte[]{0x1B, 0x61, 0x01}); // Centrar
                outputStream.write(((String) item).getBytes(StandardCharsets.UTF_8));
                outputStream.write("\n".getBytes(StandardCharsets.UTF_8));
            } else if (item instanceof Map) {
                // Si es un objeto (probablemente una imagen)
                Map<String, Object> objItem = (Map<String, Object>) item;

                if ("image".equals(objItem.get("type"))) {
                    String imageData = (String) objItem.get("data");
                    Map<String, Object> options = (Map<String, Object>) objItem.get("options");

                    // Procesar imagen base64
                    procesarImagen(outputStream, imageData, options);
                }
            }
        }

        outputStream.write(new byte[]{0x1B, 0x61, 0x01}); // Centrar
        outputStream.write("¡Gracias por su compra!\n\n\n\n".getBytes(StandardCharsets.UTF_8));
        outputStream.write(new byte[]{0x1D, 0x56, 0x41, 0x10}); // Corte de papel

        byte[] escposCommands = outputStream.toByteArray();

        // Enviar datos a la impresora
        DocPrintJob job = selectedPrinter.createPrintJob();
        Doc doc = new SimpleDoc(escposCommands, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
        job.print(doc, new HashPrintRequestAttributeSet());
    }

    private void procesarImagen(ByteArrayOutputStream outputStream, String imageData, Map<String, Object> options) throws Exception {
        try {
            // Extraer datos base64 de la URL de datos
            String base64Data = imageData;
            if (imageData.startsWith("data:")) {
                base64Data = imageData.substring(imageData.indexOf(",") + 1);
                // Si está URL encoded
                if (imageData.contains("charset=utf-8,")) {
                    base64Data = URLDecoder.decode(base64Data, StandardCharsets.UTF_8.name());
                }
            }

            // Para SVG, necesitamos convertirlo a una imagen rasterizada
            BufferedImage bufferedImage;

            if (imageData.contains("image/svg+xml")) {
                // Convertir SVG a imagen
                TranscoderInput input = new TranscoderInput(new StringReader(base64Data));
                BufferedImageTranscoder transcoder = new BufferedImageTranscoder();

                // Obtener ancho de la imagen de las opciones
                int width = options != null && options.containsKey("width") ?
                        ((Number) options.get("width")).intValue() : 200;
                transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, (float) width);

                TranscoderOutput output = new TranscoderOutput();
                transcoder.transcode(input, output);
                bufferedImage = transcoder.getBufferedImage();
            } else {
                // Procesar otras imágenes como PNG, JPG
                byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            }

            // Convertir a blanco y negro respetando la transparencia
            BufferedImage bwImage = new BufferedImage(
                    bufferedImage.getWidth(),
                    bufferedImage.getHeight(),
                    BufferedImage.TYPE_BYTE_BINARY);

            Graphics2D graphics = bwImage.createGraphics();
            // Establecer color de fondo blanco explícito para áreas transparentes
            graphics.setBackground(Color.WHITE);
            graphics.clearRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.drawImage(bufferedImage, 0, 0, null);
            graphics.dispose();

            // Centrar imagen si es necesario
            if (options != null && "center".equals(options.get("align"))) {
                outputStream.write(new byte[]{0x1B, 0x61, 0x01}); // Centrar
            }

            // Convertir a formato ESC/POS para impresora térmica
            outputStream.write(new byte[]{0x1D, 0x76, 0x30, 0x00}); // GS v 0 m

            int width = bwImage.getWidth();
            int height = bwImage.getHeight();

            // Calcular ancho en bytes (8 píxeles por byte)
            int widthBytes = (width + 7) / 8;

            // Enviar dimensiones
            outputStream.write(widthBytes & 0xFF);         // xL
            outputStream.write((widthBytes >> 8) & 0xFF);  // xH
            outputStream.write(height & 0xFF);             // yL
            outputStream.write((height >> 8) & 0xFF);      // yH

            // Enviar datos de imagen - versión mejorada para manejar transparencia
            byte[] imageBytes = new byte[widthBytes * height];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x += 8) {
                    byte b = 0;
                    for (int i = 0; i < 8; i++) {
                        if (x + i < width) {
                            int rgba = bufferedImage.getRGB(x + i, y);
                            int alpha = (rgba >> 24) & 0xff;

                            // Si el pixel tiene transparencia (alpha < 128), tratarlo como blanco
                            // Si no es transparente, usar el valor de bwImage (ya convertido a B/N)
                            if (alpha > 128) {
                                int color = bwImage.getRGB(x + i, y);
                                if ((color & 0xFF) < 128) {
                                    b |= (1 << (7 - i));
                                }
                            }
                            // Si es transparente, no hacer nada (dejarlo como blanco)
                        }
                    }
                    imageBytes[y * widthBytes + x / 8] = b;
                }
            }

            outputStream.write(imageBytes);

            // Restaurar alineación
            outputStream.write(new byte[]{0x1B, 0x61, 0x00}); // Alineación izquierda
        } catch (Exception e) {
            System.err.println("Error al procesar imagen: " + e.getMessage());
            e.printStackTrace();
            // Continuar sin la imagen
        }
    }

    // Clase auxiliar para convertir SVG a BufferedImage
    private static class BufferedImageTranscoder extends ImageTranscoder {
        private BufferedImage bufferedImage = null;

        @Override
        public BufferedImage createImage(int width, int height) {
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }

        @Override
        public void writeImage(BufferedImage image, TranscoderOutput output) {
            this.bufferedImage = image;
        }

        public BufferedImage getBufferedImage() {
            return bufferedImage;
        }
    }


    // Nuevo endpoint para obtener la lista de impresoras disponibles
    @GetMapping
    public List<String> getAvailablePrinters() {
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        List<String> printerNames = new ArrayList<>();

        for (PrintService printService : printServices) {
            String printerName = printService.getAttribute(PrinterName.class) != null
                    ? printService.getAttribute(PrinterName.class).toString()
                    : printService.getName(); // Usa el nombre interno si no hay atributo
            printerNames.add(printerName);
        }

        return printerNames;
    }

}
