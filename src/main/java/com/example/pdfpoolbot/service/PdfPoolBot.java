package com.example.pdfpoolbot.service;

import com.example.pdfpoolbot.admin.AdminService;
import com.example.pdfpoolbot.convert.DocxToPdf;
import com.example.pdfpoolbot.convert.PptToPdf;
import com.example.pdfpoolbot.convert.PptxToPdf;
import com.example.pdfpoolbot.convert.XlsxToPdf;
import com.example.pdfpoolbot.create.PdfGenerator;
import com.example.pdfpoolbot.encrypt.PdfEncryptor;
import com.example.pdfpoolbot.merge.PdfMerger;
import com.example.pdfpoolbot.state.BotState;
import com.example.pdfpoolbot.user.UserDto;
import com.example.pdfpoolbot.user.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PdfPoolBot extends TelegramLongPollingBot {

    @Value("${pdf-pool-bot.telegram.bot.token}")
    private String botToken;

    @Value("${pdf-pool-bot.telegram.bot.username}")
    private String botUsername;

    private final UserService userService;
    private final AdminService adminService;
    private final PdfGenerator pdfGenerator;
    private final PdfEncryptor pdfEncryptor;
    private final PdfMerger pdfMerger;

    private static final Logger logger = LogManager.getLogger(PdfPoolBot.class);

    private BotState currentState = BotState.DEFAULT;
    private List<byte[]> imageList = new ArrayList<>();
    private List<byte[]> pdfList = new ArrayList<>();
    private byte[] currentPdf;
    private String currentPassword;
    private String fileName;

    private String ADSPhoto;
    private String ADSText;
    private String ADSWord;
    private String ADSLink;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleTextMessage(update.getMessage());
        } else if (update.hasEditedMessage()) {
            handleEditedMessage();
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        } else if (update.hasMessage() && update.getMessage().hasPhoto()) {
            handlePhoto(update.getMessage());
        } else if (update.hasMessage() && update.getMessage().hasDocument()) {
            handleDocument(update.getMessage());
        } else {
            sendMessage(update.getMessage().getChatId(), "Kerakli so'rovni bajaring!");
        }
    }

    private void handleTextMessage(Message message) {
        String messageText = message.getText();
        long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        String firstName = message.getFrom().getFirstName();
        String userName = message.getFrom().getUserName();

        if (messageText.equals("/start")) {
            if (!userService.findByUserId(userId)) {
                userService.save(userId, firstName, userName);
            }
            sendMainMenu(chatId);
            currentState = BotState.DEFAULT;
        } else if (messageText.equals("/help")) {
            sendMessage(chatId, "Takliflar va Savollar bo'lsa @pdfpoolbotdev ushbu guruhga yozishingiz mumkin!");
        } else if (messageText.equals("/ads")) {
            if (adminService.findByAdminId(chatId)) {
                adminMenu(chatId);
                currentState = BotState.ADMIN;
            } else {
                sendMessage(chatId, "Buni faqat ADMIN bajara oladi!");
            }
        } else if (currentState == BotState.FILE_NAME) {
            fileName = messageText;
            currentState = BotState.CREATE_PDF;
            sendPhotoRequest(chatId);
        } else if (currentState == BotState.ENCODE_PDF) {
            currentPassword = messageText;
            try {
                encryptAndSendPdf(chatId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (currentState == BotState.CREATE_PDF) {
            sendPhotoRequest(chatId);
        } else if (currentState == BotState.ADMIN_TEXT) {
            ADSText = messageText;
            sendMessage(chatId, "```Text\n" + ADSText + "\n```" + "\nText qabul qilindi. Endi Link ulamoqchi bo'lgan so'zingizni kiriting. Esda tuting kiritgan so'zingiz yuborgan text ichida bo'lishi kerak!");
            currentState = BotState.ADMIN_WORD;
        } else if (currentState == BotState.ADMIN_WORD) {
            if (containsWord(ADSText, messageText)) {
                ADSWord = messageText;
                sendMessage(chatId, "So'z qabul qilindi. Endi kiritgan so'zingizga ulamoqchi bo'lgan linkingizni kiring. Esda tuting link \"https://t.me/PDFPOOLBOT\" ko'rinishida bo'lishi kerak");
                currentState = BotState.ADMIN_LINK;
            } else {
                sendMessage(chatId, messageText + " - kiritgan so'zingiz Textni ichida mavjud emas! Qaytadan kiriting.");
            }

        } else if (currentState == BotState.ADMIN_LINK) {
            ADSLink = messageText;
            broadcastPhotoAndText(ADSPhoto, ADSText, ADSWord, ADSLink);
            sendMessage(chatId, "Muvaffaqiyatli yuborildi");
            ADSPhoto = null;
            ADSText = null;
            ADSWord = null;
            ADSLink = null;
            currentState = BotState.ADMIN;
            adminMenu(chatId);
        } else if (currentState == BotState.ADMIN_DATE) {
            UserDto userCreationInfo = userService.getUserCreationInfo(messageText);
            sendMessage(chatId, messageText + " - Kunidagi aktiv userlar soni - " + userCreationInfo.getCount() + "\n" + userCreationInfo.getUsernames());
            currentState = BotState.ADMIN;
        } else {
            sendMessage(chatId, "Kerakli so'rovlarni bajaring!");
        }
    }

    private void handleEditedMessage() {
        if (currentState == BotState.CREATE_PDF) {
            logger.warn("image edited");
        } else if (currentState == BotState.FILE_NAME) {
            logger.warn("text edited");
        } else {
            logger.warn("file edited");
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();

        switch (callbackData) {
            case "CREATE_PDF" -> {
                currentState = BotState.FILE_NAME;
                sendFileNameRequest(chatId);
            }
            case "ENCODING_PDF" -> {
                currentState = BotState.ENCODE_PDF;
                sendPdfRequest(chatId);
            }
            case "ADD_PDF" -> {
                currentState = BotState.MERGE_PDF_1;
                sendPdfRequest(chatId);
            }
            case "DOCX_TO_PDF" -> {
                currentState = BotState.DOCX_TO_PDF;
                sendFileRequest(chatId, "DOCX");
            }
            case "XLSX_TO_PDF" -> {
                currentState = BotState.XLSX_TO_PDF;
                sendFileRequest(chatId, "XLSX");
            }
            case "PPTX_TO_PDF" -> {
                currentState = BotState.PPTX_TO_PDF;
                sendFileRequest(chatId, "PPTX");
            }
            case "PPT_TO_PDF" -> {
                currentState = BotState.PPT_TO_PDF;
                sendFileRequest(chatId, "PPT");
            }
            case "MENU" -> {
                currentState = BotState.DEFAULT;
                sendMainMenu(chatId);
            }
            case "ADD_PHOTO" -> sendPhotoRequest(chatId);
            case "DONE" -> {
                if (currentState == BotState.CREATE_PDF) {
                    try {
                        createAndSendPdf(chatId);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (currentState == BotState.MERGE_PDF_2) {
                    try {
                        mergeAndSendPdf(chatId);
                    } catch (IOException | TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            }
            case "PHOTO_TEXT" -> {
                currentState = BotState.ADMIN_PHOTO;
                sendMessage(chatId, "ADS uchun kerakli rasmni yuboring");
            }
            case "ALL_USERS" -> {
                UserDto userDto = userService.allUsers();
                sendMessage(chatId, "Barcha foydalanuvchilar soni - " + userDto.getCount() + "\n" + userDto.getUsernames());
            }
            case "DATE_USERS" -> {
                sendMessage(chatId, "Ma'lumot olmoqchi bo'lgan sanangizni kiriting(YYYY-MM-DD). Misol uchun: 2024-07-05");
                currentState = BotState.ADMIN_DATE;
            }
            case "PREVIOUS_DATE" -> {
                UserDto users = userService.getUserActivityInfo();
                if (users.getCount() > 0) {
                    sendMessage(chatId, "O'tgan oyga nisbatan bu oy - " + users.getCount() + " ta ko'p😊\n" + "Bu oyda aktiv bo'lgan userlar: \n" + users.getUsernames());
                } else if (users.getCount() == 0) {
                    sendMessage(chatId, "Bu oydagi farq 0 ga teng\n" + "Bu oyda aktiv bo'lgan userlar: \n" + users.getUsernames());
                } else {
                    sendMessage(chatId, "O'tgan oyga nisbatan bu oy " + users.getCount() + " ta kam😞\n" + "Bu oyda aktiv bo'lgan userlar: \n" + users.getUsernames());
                }
            }
        }
    }

    // user yuborgan rasmlarni listga saqlaydi
    private void handlePhoto(Message message) {
        if (currentState == BotState.CREATE_PDF) {
            List<PhotoSize> photos = message.getPhoto();
            if (!photos.isEmpty()) {
                PhotoSize photo = photos.get(photos.size() - 1);
                try {
                    byte[] imageData = downloadPhotoByFileId(photo.getFileId());
                    imageList.add(imageData);
                    sendAddPhotoMenu(message.getChatId());
                } catch (TelegramApiException | IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (currentState == BotState.FILE_NAME) {
            sendFileNameRequest(message.getChatId());
        } else if (currentState == BotState.ADMIN_PHOTO) {
            List<PhotoSize> photo = message.getPhoto();
            if (!photo.isEmpty()) {
                try {
                    PhotoSize photoSize = photo.get(photo.size() - 1);
                    ADSPhoto = photoSize.getFileId();
                    sendMessage(message.getChatId(), "Rasm qabul qilindi. Endi text yuboring");
                    currentState = BotState.ADMIN_TEXT;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            sendFileRequest(message.getChatId(), "");
        }
    }

    private void handleDocument(Message message) {
        Document document = message.getDocument();
        DocumentInfoExtractor.DocumentInfo documentInfo = DocumentInfoExtractor.getDocumentInfo(document);
        fileName = documentInfo.getName();
        long chatId = message.getChatId();

        try {
            byte[] fileData = downloadFileByFileId(document.getFileId());
            switch (currentState) {
                case CREATE_PDF -> {
                    saveDocumentAsPhoto(document, chatId);
                }
                case ENCODE_PDF -> {
                    currentPdf = fileData;
                    sendMessage(chatId, "Parol kiriting:");
                }
                case MERGE_PDF_1 -> {
                    pdfList.add(fileData);
                    currentState = BotState.MERGE_PDF_2;
                    sendMessage(chatId, "Yaxshi, Endi 2-PDF faylingizni yuboring:");
                }
                case MERGE_PDF_2 -> {
                    pdfList.add(fileData);
                    sendDoneEndMenu(chatId);
                }
                case DOCX_TO_PDF -> {
                    if (documentInfo.getFormat().equals("docx")) {
                        byte[] pdfData = DocxToPdf.docxToPdf(fileData);
                        sendConvertedFile(chatId, pdfData, documentInfo.getName());

                    } else {
                        sendMessage(chatId, "Ushbu fayl .DOCX formatida emas. Iltimos, .DOCX fayl yuboring.");
                    }
                }
                case XLSX_TO_PDF -> {
                    if (documentInfo.getFormat().equals("xlsx")) {
                        byte[] pdfData = XlsxToPdf.xlsxToPdf(fileData);
                        sendConvertedFile(chatId, pdfData, documentInfo.getName());
                    } else {
                        sendMessage(chatId, "Ushbu fayl .XLSX formatida emas. Iltimos, .XLSX fayl yuboring.");
                    }
                }
                case PPTX_TO_PDF -> {
                    if (documentInfo.getFormat().equals("pptx")) {
                        byte[] pdfData = PptxToPdf.pptxToPdf(fileData);
                        sendConvertedFile(chatId, pdfData, documentInfo.getName());
                    } else {
                        sendMessage(chatId, "Ushbu fayl .PPTX formatida emas. Iltimos, .PPTX fayl yuboring.");
                    }
                }
                case PPT_TO_PDF -> {
                    if (documentInfo.getFormat().equals("ppt")) {
                        byte[] pdfData = PptToPdf.pptToPdf(fileData);
                        sendConvertedFile(chatId, pdfData, documentInfo.getName());
                    } else {
                        sendMessage(chatId, "Ushbu fayl .PPT formatida emas. Iltimos, .PPT fayl yuboring.");
                    }
                }
                case ADMIN_PHOTO -> {
                    saveDocumentAsPhotoAdmin(document, chatId);
                    sendMessage(chatId, "Rasm qabul qilindi. Endi text yuboring");
                    currentState = BotState.ADMIN_TEXT;
                }
                default -> {
                    sendMessage(chatId, "Hozirda bu amalni bajarish uchun boshqa holat yo'q.");
                    sendMainMenu(chatId);
                }
            }
        } catch (TelegramApiException | IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Userdan fayl nomini yuborishini so'raydi
    private void sendFileNameRequest(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        if (currentState == BotState.FILE_NAME) {
            message.setText("File nomini kiriting:\nMisol uchun: text, name_surname");
        }
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    //userdan pdf fayl yuborishini so'raydi
    private void sendPdfRequest(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Iltimos shifrlanmagan PDF fayl yuboring:");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    //  userdan kerakli fayl yuborishini so'raydi
    private void sendFileRequest(long chatId, String fileType) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(fileType + " File yuboring.");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Userga menuni yuboradi
    private void sendMainMenu(long chatId) {
        SendMessage pdfButtonMessage = new SendMessage();
        pdfButtonMessage.setChatId(chatId);
        pdfButtonMessage.setText("Botdan foydalanishingiz mumkin😊");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(createButton("PDF yaratish", "CREATE_PDF"));
        row.add(createButton("PDFni kodlash", "ENCODING_PDF"));

        rows.add(row);
        row = new ArrayList<>();
        row.add(createButton("PDF larni birlashtirish", "ADD_PDF"));

        rows.add(row);
        row = new ArrayList<>();
        row.add(createButton("PPTX to PDF", "PPTX_TO_PDF"));
        row.add(createButton("PPT to PDF", "PPT_TO_PDF"));

        rows.add(row);
        row = new ArrayList<>();
        row.add(createButton("DOCX to PDF", "DOCX_TO_PDF"));
        row.add(createButton("XLSX to PDF", "XLSX_TO_PDF"));

        rows.add(row);
        markup.setKeyboard(rows);
        pdfButtonMessage.setReplyMarkup(markup);

        try {
            execute(pdfButtonMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // buttonlarni yaratadi
    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

    // Userdan rasm yuborishini so'raydi
    private void sendPhotoRequest(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        if (currentState == BotState.CREATE_PDF) {
            message.setText("Rasm yuboring:");
        }
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // User yuborgan rasmni qabul qiladi va kerakli so'rov yuboradi
    private void sendAddPhotoMenu(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Rasm qabul qilindi.\nAgar yana rasm qo'shmoqchi bo'lsangiz \"Add Photo\" ni bosing.\nBarcha rasmlarni yuborib bo'lgach \"Tayyor\" tugmasini bosing.");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(createButton("Add Photo", "ADD_PHOTO"));
        row.add(createButton("Tayyor", "DONE"));

        rows.add(row);
        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendDoneEndMenu(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Qabul qilindi \"Tayyor\".\nMenuga qaytish \"Menu\".");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(createButton("Menu", "MENU"));
        row.add(createButton("Tayyor", "DONE"));

        rows.add(row);
        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // rasmlardan pdf yaratib userga yuboradi
    private void createAndSendPdf(long chatId) throws IOException {
        byte[] pdfData = pdfGenerator.createPdf(imageList);

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId);
        sendDocument.setCaption("Ushbu pdf file @PDFPOOLBOT yordamida yaratildi!");
        sendDocument.setDocument(new InputFile(new ByteArrayInputStream(pdfData), fileName + ".pdf"));

        try {
            execute(sendDocument);
            sendMainMenu(chatId);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        imageList.clear();
        currentState = BotState.DEFAULT;
    }

    // pdf faylni shifrlab userga yuborish
    private void encryptAndSendPdf(long chatId) throws Exception {
        PDDocument document = null;
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);

        try {
            document = PDDocument.load(currentPdf);

            // Encrypt the PDF
            byte[] encryptedPdf = pdfEncryptor.encryptPdf(currentPdf, currentPassword);
            SendDocument sendDocument = new SendDocument();
            sendDocument.setChatId(chatId);
            sendDocument.setCaption("Parol: " + currentPassword + "\nUshbu kodlash @PDFPOOLBOT yordamida amalga oshirildi");
            sendDocument.setDocument(new InputFile(new ByteArrayInputStream(encryptedPdf), fileName + ".pdf"));

            // Send the encrypted PDF
            execute(sendDocument);
            sendMainMenu(chatId);

        } catch (InvalidPasswordException e) {
            e.printStackTrace();
            sendMessage.setText("Bu pdf fayl allaqachon kodlangan!");
            execute(sendMessage);
            sendMainMenu(chatId);
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage.setText("Xatolik yuz berdi. Iltimos, qayta urinib ko'ring.");
            execute(sendMessage);
            sendMainMenu(chatId);
        } finally {
            if (document != null) {
                document.close();
            }
            fileName = null;
            currentPdf = null;
            currentPassword = null;
            currentState = BotState.DEFAULT;
        }
    }

    // pdf fayllarni bilashtirib userga yuborish
    private void mergeAndSendPdf(long chatId) throws IOException, TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        SendDocument sendDocument = new SendDocument();

        try {
            byte[] mergedPdf = pdfMerger.mergePdfs(pdfList);

            for (byte[] pdfData : pdfList) {
                try (PDDocument document = PDDocument.load(pdfData)) {
                    if (document.isEncrypted()) {
                        break;
                    }
                }
            }

            sendDocument.setChatId(chatId);
            sendDocument.setCaption("Ushbu birlashtirish @PDFPOOLBOT yordamida amalga oshirildi\n");
            sendDocument.setDocument(new InputFile(new ByteArrayInputStream(mergedPdf), "merged-file.pdf"));
            execute(sendDocument);
            sendMainMenu(chatId);
        } catch (InvalidPasswordException e) {
            e.printStackTrace();
            sendMessage.setText("Ushbu fayllar shifrlangan. Iltimos, shifrlanmagan fayllar yuboring!");
            execute(sendMessage);
            sendMainMenu(chatId);
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage.setText("Xatolik yuz berdi. Iltimos qaytadan urinib ko'ring!");
            execute(sendMessage);
            sendMainMenu(chatId);
        } finally {
            pdfList.clear();
            currentState = BotState.DEFAULT;
        }
    }


    //  user yuborgan documentli rasmni documtentdan ajratib olib listga saqlaydi
    private void saveDocumentAsPhoto(Document document, Long chatId) {
        String mimeType = document.getMimeType();
        try {
            if (mimeType.startsWith("image/")) {
                String fileId = document.getFileId();

                GetFile getFileMethod = new GetFile();
                getFileMethod.setFileId(fileId);

                File file = execute(getFileMethod);
                java.io.File downloadedFile = downloadFile(file);

                byte[] imageData = readBytesFromFile(downloadedFile);
                imageList.add(imageData);

                sendAddPhotoMenu(chatId);
            } else {
                sendMessage(chatId, "Ushbu rasmni saqlab bo'lmadi!");
            }
        } catch (TelegramApiException | IOException e) {
            sendMessage(chatId, "Ushbu rasmni saqlab bo'lmadi!");
            sendMainMenu(chatId);
            currentState = BotState.DEFAULT;
        }
    }

    private void saveDocumentAsPhotoAdmin(Document document, Long chatId) {
        String mimeType = document.getMimeType();
        try {
            if (mimeType.startsWith("image/")) {
                String fileId = document.getFileId();

                GetFile getFileMethod = new GetFile();
                getFileMethod.setFileId(fileId);

                File file = execute(getFileMethod);
                java.io.File downloadedFile = downloadFile(file);
                ADSPhoto = downloadedFile.getPath();
            } else {
                sendMessage(chatId, "Ushbu rasmni saqlab bo'lmadi!");
            }
        } catch (TelegramApiException e) {
            sendMessage(chatId, "Ushbu rasmni saqlab bo'lmadi!");
            adminMenu(chatId);
            currentState = BotState.ADMIN;
        }
    }

    // user yuborgan documentli rasmni bytega aylantiradi
    private byte[] readBytesFromFile(java.io.File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    // rasmni bytega aylantiradi
    private byte[] downloadPhotoByFileId(String fileId) throws TelegramApiException, IOException {
        GetFile getFile = new GetFile();
        getFile.setFileId(fileId);
        File file = execute(getFile);
        java.io.File downloadedFile = downloadFile(file);
        return Files.readAllBytes(downloadedFile.toPath());
    }

    // faylni bytega aylantiradi
    private byte[] downloadFileByFileId(String fileId) throws TelegramApiException, IOException {
        GetFile getFile = new GetFile();
        getFile.setFileId(fileId);
        File file = execute(getFile);
        java.io.File downloadedFile = downloadFile(file);
        return Files.readAllBytes(downloadedFile.toPath());
    }

    //  userga convert bo'lgan faylni yuboradi
    private void sendConvertedFile(long chatId, byte[] fileData, String fileName) {

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId);
        sendDocument.setDocument(new InputFile(new ByteArrayInputStream(fileData), fileName + ".pdf"));
        sendDocument.setCaption("Ushbu convertatsiya @PDFPOOLBOT orqali amalga oshirildi!");

        try {
            execute(sendDocument);
            sendMainMenu(chatId);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        currentState = BotState.DEFAULT;
    }

    // Userga kerakli message yuboradi
    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // ----------------- ADMIN ---------------
    private void adminMenu(long chatId) {
        SendMessage pdfButtonMessage = new SendMessage();
        pdfButtonMessage.setChatId(chatId);
        pdfButtonMessage.setText("Xush kelibsiz!!!");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(createButton("Photo with Text", "PHOTO_TEXT"));

        rows.add(row);
        row = new ArrayList<>();
        row.add(createButton("Kunlik Userlar", "DATE_USERS"));

        rows.add(row);
        row = new ArrayList<>();
        row.add(createButton("Oylik Natija", "PREVIOUS_DATE"));

        rows.add(row);
        row = new ArrayList<>();
        row.add(createButton("All Users", "ALL_USERS"));

        rows.add(row);
        markup.setKeyboard(rows);
        pdfButtonMessage.setReplyMarkup(markup);

        try {
            execute(pdfButtonMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void broadcastPhotoAndText(String photo, String text, String word, String link) {
        List<SendPhoto> sendPhotos = adminService.broadcastPhotoWithText(photo, text, word, link);
        for (SendPhoto sendPhoto : sendPhotos) {
            try {
                execute(sendPhoto);
            } catch (TelegramApiException e) {
                logger.warn(e.getMessage());
            }
        }
    }

    private boolean containsWord(String text, String word) {
        if (text == null || word == null) {
            return false;
        }
        return text.contains(word);
    }


    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}
