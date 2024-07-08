package com.example.pdfpoolbot.encrypt;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PdfEncryptor {

    public byte[] encryptPdf(byte[] pdfData, String password) throws Exception {
        PDDocument document = PDDocument.load(pdfData);
        AccessPermission accessPermission = new AccessPermission();
        StandardProtectionPolicy protectionPolicy = new StandardProtectionPolicy(password, password, accessPermission);
        protectionPolicy.setEncryptionKeyLength(128);
        protectionPolicy.setPermissions(accessPermission);

        document.protect(protectionPolicy);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        document.save(outputStream);
        document.close();
        return outputStream.toByteArray();
    }


}
