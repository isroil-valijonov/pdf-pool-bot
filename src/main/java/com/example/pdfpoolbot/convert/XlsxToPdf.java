package com.example.pdfpoolbot.convert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;


public class XlsxToPdf {

    public static byte[] xlsxToPdf(byte[] xlsxData) throws IOException, DocumentException {

        // Create an input stream from the byte array
        ByteArrayInputStream inputStream = new ByteArrayInputStream(xlsxData);

        // Create an XSSFWorkbook object from the input stream
        XSSFWorkbook workbook = new XSSFWorkbook(inputStream);

        // Create a ByteArrayOutputStream to write the PDF data to
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Create a PDF document
        Document document = new Document();
        PdfWriter.getInstance(document, outputStream);

        document.open();

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            XSSFSheet worksheet = workbook.getSheetAt(i);

            // Add header with sheet name as title
            Paragraph title = new Paragraph(worksheet.getSheetName(), new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD));
            title.setSpacingAfter(20f);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            createAndAddTable(worksheet, document);

            // Add a new page for each sheet (except the last one)
            if (i < workbook.getNumberOfSheets() - 1) {
                document.newPage();
            }
        }

        document.close();
        workbook.close();
        inputStream.close();

        // Return the PDF as a byte array
        return outputStream.toByteArray();
    }

    private static void createAndAddTable(XSSFSheet worksheet, Document document) {
        try {
            Row firstRow = worksheet.getRow(0);
            if (firstRow == null) {
                return;
            }
            PdfPTable table = new PdfPTable(firstRow.getPhysicalNumberOfCells());
            table.setWidthPercentage(100);
            addTableHeader(worksheet, table);
            addTableData(worksheet, table);
            document.add(table);
        } catch (DocumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void addTableHeader(XSSFSheet worksheet, PdfPTable table) {
        try {
            Row headerRow = worksheet.getRow(0);
            if (headerRow != null) {
                for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
                    Cell cell = headerRow.getCell(i);
                    if (cell != null) {
                        String cellValue = getCellText(cell);
                        PdfPCell cellPdf = new PdfPCell(new Phrase(cellValue, getCellStyle(cell)));
                        setBackgroundColor(cell, cellPdf);
                        setCellAlignment(cell, cellPdf);
                        table.addCell(cellPdf);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static void addTableData(XSSFSheet worksheet, PdfPTable table) {
        for (Row row : worksheet) {
            if (row.getRowNum() == 0) {
                continue;
            }
            for (int i = 0; i < row.getPhysicalNumberOfCells(); i++) {
                Cell cell = row.getCell(i);
                String cellValue = getCellText(cell);
                PdfPCell cellPdf = new PdfPCell(new Phrase(cellValue, getCellStyle(cell)));
                setBackgroundColor(cell, cellPdf);
                setCellAlignment(cell, cellPdf);
                table.addCell(cellPdf);
            }
        }
    }

    private static String getCellText(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(BigDecimal.valueOf(cell.getNumericCellValue()));
            default -> "";
        };
    }

    private static void setBackgroundColor(Cell cell, PdfPCell cellPdf) {
        // Set background color
        short bgColorIndex = cell.getCellStyle().getFillForegroundColor();
        if (bgColorIndex != IndexedColors.AUTOMATIC.getIndex()) {
            XSSFColor bgColor = (XSSFColor) cell.getCellStyle().getFillForegroundColorColor();
            if (bgColor != null) {
                byte[] rgb = bgColor.getRGB();
                if (rgb != null && rgb.length == 3) {
                    cellPdf.setBackgroundColor(new BaseColor(rgb[0] & 0xFF, rgb[1] & 0xFF, rgb[2] & 0xFF));
                }
            }
        }
    }

    private static void setCellAlignment(Cell cell, PdfPCell cellPdf) {
        CellStyle cellStyle = cell.getCellStyle();

        HorizontalAlignment horizontalAlignment = cellStyle.getAlignment();
        VerticalAlignment verticalAlignment = cellStyle.getVerticalAlignment();

        switch (horizontalAlignment) {
            case LEFT -> cellPdf.setHorizontalAlignment(Element.ALIGN_LEFT);
            case CENTER -> cellPdf.setHorizontalAlignment(Element.ALIGN_CENTER);
            case JUSTIFY, FILL -> cellPdf.setVerticalAlignment(Element.ALIGN_JUSTIFIED);
            case RIGHT -> cellPdf.setHorizontalAlignment(Element.ALIGN_RIGHT);
        }

        switch (verticalAlignment) {
            case TOP -> cellPdf.setVerticalAlignment(Element.ALIGN_TOP);
            case CENTER -> cellPdf.setVerticalAlignment(Element.ALIGN_MIDDLE);
            case JUSTIFY -> cellPdf.setVerticalAlignment(Element.ALIGN_JUSTIFIED);
            case BOTTOM -> cellPdf.setVerticalAlignment(Element.ALIGN_BOTTOM);
        }
    }

    private static Font getCellStyle(Cell cell) {
        Font font = new Font();
        CellStyle cellStyle = cell.getCellStyle();
        org.apache.poi.ss.usermodel.Font cellFont = cell.getSheet().getWorkbook().getFontAt(cellStyle.getFontIndexAsInt());

        short fontColorIndex = cellFont.getColor();
        if (fontColorIndex != IndexedColors.AUTOMATIC.getIndex() && cellFont instanceof XSSFFont) {
            XSSFColor fontColor = ((XSSFFont) cellFont).getXSSFColor();
            if (fontColor != null) {
                byte[] rgb = fontColor.getRGB();
                if (rgb != null && rgb.length == 3) {
                    font.setColor(new BaseColor(rgb[0] & 0xFF, rgb[1] & 0xFF, rgb[2] & 0xFF));
                }
            }
        }

        if (cellFont.getItalic()) {
            font.setStyle(Font.ITALIC);
        }

        if (cellFont.getStrikeout()) {
            font.setStyle(Font.STRIKETHRU);
        }

        if (cellFont.getUnderline() == 1) {
            font.setStyle(Font.UNDERLINE);
        }

        short fontSize = cellFont.getFontHeightInPoints();
        font.setSize(fontSize);

        if (cellFont.getBold()) {
            font.setStyle(Font.BOLD);
        }

        String fontName = cellFont.getFontName();
        if (FontFactory.isRegistered(fontName)) {
            font.setFamily(fontName);
        } else {
            font.setFamily(FontFactory.HELVETICA);
        }

        return font;
    }

}
