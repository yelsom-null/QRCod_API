package qrcodeapi;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@RestController
public class ApiController {

    @GetMapping("/api/health")
    public ResponseEntity<String> healthCheck() {
        return new ResponseEntity<>("Healthy", HttpStatus.OK);
    }

    @GetMapping(path = "/api/qrcode")
    public ResponseEntity<?> generateQRCode(
            @RequestParam String contents,
            @RequestParam(required = false, defaultValue = "250") int size,
            @RequestParam(required = false, defaultValue = "png") String type,
            @RequestParam(required = false, defaultValue = "L") String correction) throws IOException {


        if (contents == null || contents.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Contents cannot be null or blank"));
        }


        if (size < 150 || size > 350) {
            return ResponseEntity.badRequest().body(Map.of("error", "Image size must be between 150 and 350 pixels"));
        }


        ErrorCorrectionLevel errorCorrectionLevel;
        switch (correction.toUpperCase()) {
            case "L": errorCorrectionLevel = ErrorCorrectionLevel.L; break;
            case "M": errorCorrectionLevel = ErrorCorrectionLevel.M; break;
            case "Q": errorCorrectionLevel = ErrorCorrectionLevel.Q; break;
            case "H": errorCorrectionLevel = ErrorCorrectionLevel.H; break;
            default: return ResponseEntity.badRequest().body(Map.of("error", "Permitted error correction levels are L, M, Q, H"));
        }


        if (!("png".equalsIgnoreCase(type) || "jpeg".equalsIgnoreCase(type) || "gif".equalsIgnoreCase(type))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only png, jpeg and gif image types are supported"));
        }

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, ErrorCorrectionLevel> hints = Map.of(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);
            BitMatrix bitMatrix = qrCodeWriter.encode(contents, BarcodeFormat.QR_CODE, size, size, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, type, baos);

            MediaType mediaType = switch (type.toLowerCase()) {
                case "png" -> MediaType.IMAGE_PNG;
                case "jpeg" -> MediaType.IMAGE_JPEG;
                default -> MediaType.valueOf("image/gif");
            };

            return ResponseEntity
                    .ok()
                    .contentType(mediaType)
                    .body(baos.toByteArray());
        } catch (WriterException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error generating QR code"));
        }
    }
}
