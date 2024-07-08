package com.example.pdfpoolbot.convert;

import fr.opensagres.poi.xwpf.converter.pdf.PdfConverter;
import fr.opensagres.poi.xwpf.converter.pdf.PdfOptions;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class DocxToPdf {

    public static byte[] docxToPdf(byte[] fileData) throws Exception {

        ByteArrayInputStream inputStream = new ByteArrayInputStream(fileData);
        XWPFDocument doc = new XWPFDocument(inputStream);

        PdfOptions pdfOptions = PdfOptions.create();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PdfConverter.getInstance().convert(doc, outputStream, pdfOptions);

        doc.close();
        outputStream.close();
        return outputStream.toByteArray();

    }
}
