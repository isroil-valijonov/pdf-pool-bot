package com.example.pdfpoolbot.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final AdminService adminService;

    @Override
    public void run(String... args)  {

        try {
            if (!adminService.findByAdminId(773099413L)) {
                adminService.save(773099413L);
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

    }

}
