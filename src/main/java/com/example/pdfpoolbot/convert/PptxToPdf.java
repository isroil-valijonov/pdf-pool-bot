package com.example.pdfpoolbot.convert;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class PptxToPdf {
    public static byte[] pptxToPdf(byte[] pptData) throws Exception {

        ByteArrayInputStream inputStream = new ByteArrayInputStream(pptData);

        XMLSlideShow ppt = new XMLSlideShow(inputStream);

        Dimension pagesize = ppt.getPageSize();

        int numSlides = ppt.getSlides().size();

        // Create a Document object with A4 page size
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, outputStream);

        document.open();

        // Set the zoom factor for scaling the slide images
        double zoom = 2;
        AffineTransform at = new AffineTransform();
        at.setToScale(zoom, zoom);

        for (int i = 0; i < numSlides; i++) {
            XSLFSlide slide = ppt.getSlides().get(i);

            // Create a BufferedImage to draw the slide
            BufferedImage bufImg = new BufferedImage(
                    (int) Math.ceil(pagesize.width * zoom),
                    (int) Math.ceil(pagesize.height * zoom),
                    BufferedImage.TYPE_INT_RGB
            );
            Graphics2D graphics = bufImg.createGraphics();
            graphics.setTransform(at);

            // Set the background color of the slide
            graphics.setPaint(slide.getBackground().getFillColor());
            graphics.fill(new Rectangle2D.Float(0, 0, pagesize.width, pagesize.height));

            try {
                // Draw the slide onto the graphics object
                slide.draw(graphics);
            } finally {
                graphics.dispose();
            }

            // Convert the BufferedImage to an iText Image
            Image image = Image.getInstance(bufImg, null);
            document.setPageSize(new Rectangle((int) image.getScaledWidth(), (int) image.getScaledHeight()));
            document.newPage();
            image.setAbsolutePosition(0, 0);
            document.add(image);
        }

        document.close();
        writer.close();
        inputStream.close();

        // Return the PDF as a byte array
        return outputStream.toByteArray();
    }
}
