package com.hampcode.ticketsales.controller;

import com.hampcode.ticketsales.dto.request.LoginRequestDTO;
import com.hampcode.ticketsales.dto.request.RegisterOrganizerRequestDTO;
import com.hampcode.ticketsales.dto.request.RegisterParticipantRequestDTO;
import com.hampcode.ticketsales.dto.response.LoginResponseDTO;
import com.hampcode.ticketsales.dto.response.OrganizerResponseDTO;
import com.hampcode.ticketsales.dto.response.ParticipantResponseDTO;
import com.hampcode.ticketsales.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register/organizer")
    public ResponseEntity<OrganizerResponseDTO> registerOrganizer(
            @Valid @RequestBody RegisterOrganizerRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerOrganizer(request));
    }

    @PostMapping("/register/participant")
    public ResponseEntity<ParticipantResponseDTO> registerParticipant(
            @Valid @RequestBody RegisterParticipantRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerParticipant(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.status(HttpStatus.OK).body(authService.login(request));
    }
}
