package com.reservestudio.controller;

import com.reservestudio.config.AuthInterceptor;
import com.reservestudio.dto.Dtos.BookingRequest;
import com.reservestudio.dto.Dtos.BookingResponse;
import com.reservestudio.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@RequestAttribute(AuthInterceptor.REQUEST_USER_ID) Long userId,
                                  @Valid @RequestBody BookingRequest request) {
        return bookingService.create(userId, request.slotStart());
    }

    @GetMapping("/my")
    public List<BookingResponse> myBookings(@RequestAttribute(AuthInterceptor.REQUEST_USER_ID) Long userId) {
        return bookingService.findByUser(userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@RequestAttribute(AuthInterceptor.REQUEST_USER_ID) Long userId,
                       @PathVariable Long id) {
        bookingService.cancel(userId, id);
    }
}
