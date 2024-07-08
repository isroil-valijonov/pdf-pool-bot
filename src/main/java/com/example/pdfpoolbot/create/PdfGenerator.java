package com.example.pdfpoolbot.create;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class PdfGenerator {

    public byte[] createPdf(List<byte[]> imageList) throws IOException {
        PDDocument document = new PDDocument();

        if (imageList != null) {
            for (byte[] imageData : imageList) {
                PDPage imagePage = new PDPage(PDRectangle.A4);
                document.addPage(imagePage);

                PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imageData, "image");

                float pageWidth = PDRectangle.A4.getWidth();
                float pageHeight = PDRectangle.A4.getHeight();
                float imageWidth = pdImage.getWidth();
                float imageHeight = pdImage.getHeight();

                float scaledWidth = imageWidth;
                float scaledHeight = imageHeight;

                // Rasmni pagedan kattaligini hisoblash
                if (imageWidth > pageWidth || imageHeight > pageHeight) {
                    float scale = Math.min(pageWidth / imageWidth, pageHeight / imageHeight);
                    scaledWidth = imageWidth * scale;
                    scaledHeight = imageHeight * scale;
                }

                // Rasmni pageni markaziga joylashtirish
                float x = (pageWidth - scaledWidth) / 2;
                float y = (pageHeight - scaledHeight) / 2;

                PDPageContentStream imageContentStream = new PDPageContentStream(document, imagePage, PDPageContentStream.AppendMode.APPEND, true, true);
                imageContentStream.drawImage(pdImage, x, y, scaledWidth, scaledHeight);
                imageContentStream.close();
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        document.save(outputStream);
        document.close();

        return outputStream.toByteArray();
    }
}
