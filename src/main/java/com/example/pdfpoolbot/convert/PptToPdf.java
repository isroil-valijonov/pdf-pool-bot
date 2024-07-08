package com.example.pdfpoolbot.convert;

import com.itextpdf.text.Rectangle;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class PptToPdf {
    public static byte[] pptToPdf(byte[] pptData) throws Exception {

        ByteArrayInputStream inputStream = new ByteArrayInputStream(pptData);

        HSLFSlideShow ppt = new HSLFSlideShow(inputStream);
        Dimension pgsize = ppt.getPageSize();
        int numSlides = ppt.getSlides().size();

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, outputStream);

        document.open();

        double zoom = 2;
        AffineTransform at = new AffineTransform();
        at.setToScale(zoom, zoom);

        for (int i = 0; i < numSlides; i++) {
            HSLFSlide slide = ppt.getSlides().get(i);

            // Create a BufferedImage to draw the slide
            BufferedImage bufImg = new BufferedImage((int) Math.ceil(pgsize.width * zoom),
                    (int) Math.ceil(pgsize.height * zoom), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = bufImg.createGraphics();
            graphics.setTransform(at);

            graphics.setPaint(slide.getBackground().getFill().getForegroundColor());
            graphics.fill(new Rectangle2D.Float(0, 0, pgsize.width, pgsize.height));

            try {
                slide.draw(graphics);
            } finally {
                graphics.dispose();
            }

            Image image = Image.getInstance(bufImg, null);
            document.setPageSize(new Rectangle((int) image.getScaledWidth(), (int) image.getScaledHeight()));
            document.newPage();
            image.setAbsolutePosition(0, 0);
            document.add(image);
        }

        document.close();
        writer.close();
        inputStream.close();

        return outputStream.toByteArray();
    }
}
