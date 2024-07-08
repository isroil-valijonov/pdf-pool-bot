package com.example.pdfpoolbot.merge;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;

@Service
public class PdfMerger {
    public byte[] mergePdfs(List<byte[]> pdfList) throws IOException {
        PDFMergerUtility pdfMerger = new PDFMergerUtility();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        for (byte[] pdfData : pdfList) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfData);
            pdfMerger.addSource(inputStream);
        }

        pdfMerger.setDestinationStream(outputStream);
        pdfMerger.mergeDocuments(null);

        return outputStream.toByteArray();
    }
}
