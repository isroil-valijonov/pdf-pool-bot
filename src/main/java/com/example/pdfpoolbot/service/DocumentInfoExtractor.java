package com.example.pdfpoolbot.service;

import org.telegram.telegrambots.meta.api.objects.Document;

public class DocumentInfoExtractor {

    public static DocumentInfo getDocumentInfo(Document document) {
        String fullName = document.getFileName();
        if (fullName == null) {
            fullName = getFileNameFromFileId(document.getFileId());
        }

        String name = getFileNameWithoutExtension(fullName);
        String format = getFileExtension(fullName);

        return new DocumentInfo(name, format);
    }

    private static String getFileNameFromFileId(String fileId) {

        return "default";
    }

    private static String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        }
        return "";
    }

    private static String getFileNameWithoutExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(0, fileName.lastIndexOf('.'));
        }
        return fileName;
    }

    public static class DocumentInfo {
        private final String name;
        private final String format;

        public DocumentInfo(String name, String format) {
            this.name = name;
            this.format = format;
        }

        public String getName() {
            return name;
        }

        public String getFormat() {
            return format;
        }

        @Override
        public String toString() {
            return "DocumentInfo{" +
                    "name='" + name + '\'' +
                    ", format='" + format + '\'' +
                    '}';
        }
    }

}

