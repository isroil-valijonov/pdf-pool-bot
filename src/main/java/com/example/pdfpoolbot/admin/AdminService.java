package com.example.pdfpoolbot.admin;

import com.example.pdfpoolbot.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final AdminRepository adminRepository;
    private final UserService userService;

    public boolean findByAdminId(Long adminId) {
        Optional<Admin> admins = adminRepository.findById(adminId);
        return admins.isPresent();
    }

    public void save(Long id) {
        Admin admin = new Admin(id);
        adminRepository.save(admin);
    }

    public List<SendPhoto> broadcastPhotoWithText(String photoId, String text, String word, String link) {
        List<Long> userIds = userService.userIdList();
        List<Long> allAdminId = adminRepository.getAllAdminId();

        userIds.removeAll(allAdminId);

        List<SendPhoto> sendPhotos = new ArrayList<>();
        String adsWithLink = text.replace(word, "[" + word + "](" + link + ")");

        for (Long userId : userIds) {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(userId);
            sendPhoto.setPhoto(new InputFile(photoId));
            sendPhoto.setCaption(adsWithLink);
            sendPhoto.setParseMode("Markdown");
            sendPhotos.add(sendPhoto);
        }

        return sendPhotos;
    }
}
