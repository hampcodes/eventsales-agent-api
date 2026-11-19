package com.hampcode.ticketsales.controller;

import com.hampcode.ticketsales.dto.request.PaymentRequestDTO;
import com.hampcode.ticketsales.dto.response.PaymentResponseDTO;
import com.hampcode.ticketsales.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PreAuthorize("hasRole('PARTICIPANT')")
    @PostMapping
    public ResponseEntity<PaymentResponseDTO> register(@Valid @RequestBody PaymentRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.registerPayment(request));
    }
}
