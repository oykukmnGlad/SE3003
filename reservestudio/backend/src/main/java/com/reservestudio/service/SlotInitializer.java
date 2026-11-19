package com.reservestudio.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Uygulama açılırken önümüzdeki günlerin slotlarını slots tablosuna yazar. */
@Component
public class SlotInitializer implements ApplicationRunner {

    private final SlotService slotService;

    public SlotInitializer(SlotService slotService) {
        this.slotService = slotService;
    }

    @Override
    public void run(ApplicationArguments args) {
        slotService.ensureWindow();
    }
}
