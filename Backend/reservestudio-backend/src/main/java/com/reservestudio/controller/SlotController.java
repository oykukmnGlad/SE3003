package com.reservestudio.controller;

import com.reservestudio.dto.Dtos.SlotResponse;
import com.reservestudio.service.SlotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
public class SlotController {

    private final SlotService slotService;

    public SlotController(SlotService slotService) {
        this.slotService = slotService;
    }

    /** GET /api/slots?date=2026-11-02  (date verilmezse bugün) */
    @GetMapping
    public List<SlotResponse> slots(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return slotService.getSlots(date != null ? date : LocalDate.now());
    }
}
