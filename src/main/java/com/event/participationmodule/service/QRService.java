package com.event.participationmodule.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class QRService {

    /**
     * Renders the QR code as PNG bytes in memory.
     * Previously every QR was written to one shared file ("email-qr.png"), so concurrent
     * registrations could overwrite each other and email a user someone else's code.
     */
    public byte[] generateQRCode(String data) throws Exception {
        BitMatrix matrix = new MultiFormatWriter()
                .encode(data, BarcodeFormat.QR_CODE, 300, 300);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        }
    }
}
