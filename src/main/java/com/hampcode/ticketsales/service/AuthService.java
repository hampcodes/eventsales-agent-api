package com.hampcode.ticketsales.service;

import com.hampcode.ticketsales.dto.request.LoginRequestDTO;
import com.hampcode.ticketsales.dto.request.OrganizerRequestDTO;
import com.hampcode.ticketsales.dto.request.ParticipantRequestDTO;
import com.hampcode.ticketsales.dto.request.RegisterOrganizerRequestDTO;
import com.hampcode.ticketsales.dto.request.RegisterParticipantRequestDTO;
import com.hampcode.ticketsales.dto.response.LoginResponseDTO;
import com.hampcode.ticketsales.dto.response.OrganizerResponseDTO;
import com.hampcode.ticketsales.dto.response.ParticipantResponseDTO;
import com.hampcode.ticketsales.entity.Organizer;
import com.hampcode.ticketsales.entity.Participant;
import com.hampcode.ticketsales.entity.Role;
import com.hampcode.ticketsales.entity.User;
import com.hampcode.ticketsales.exception.DuplicateResourceException;
import com.hampcode.ticketsales.exception.InvalidCredentialsException;
import com.hampcode.ticketsales.repository.OrganizerRepository;
import com.hampcode.ticketsales.repository.ParticipantRepository;
import com.hampcode.ticketsales.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final OrganizerRepository organizerRepository;
    private final ParticipantRepository participantRepository;
    private final OrganizerService organizerService;
    private final ParticipantService participantService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public OrganizerResponseDTO registerOrganizer(RegisterOrganizerRequestDTO request) {
        User user = createUser(request.email(), request.password(), Role.ORGANIZER);
        return organizerService.registerOrganizer(new OrganizerRequestDTO(
                request.name(), request.documentId(), user.getEmail(), request.phone(), request.website()), user);
    }

    @Transactional
    public ParticipantResponseDTO registerParticipant(RegisterParticipantRequestDTO request) {
        User user = createUser(request.email(), request.password(), Role.PARTICIPANT);
        return participantService.registerParticipant(new ParticipantRequestDTO(
                request.name(), request.documentId(), user.getEmail(), request.phone()), user);
    }

    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(normalize(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .orElseThrow(InvalidCredentialsException::new);
        Long profileId = switch (user.getRole()) {
            case ORGANIZER -> organizerRepository.findByUser_Id(user.getId()).map(Organizer::getId).orElse(null);
            case PARTICIPANT -> participantRepository.findByUser_Id(user.getId()).map(Participant::getId).orElse(null);
        };
        return new LoginResponseDTO(jwtService.generate(user), "Bearer",
                jwtService.expiresInSeconds(), user.getRole().name(), profileId);
    }

    private User createUser(String email, String password, Role role) {
        String normalized = normalize(email);
        if (userRepository.existsByEmail(normalized)) {
            throw new DuplicateResourceException("El email ya está registrado");
        }
        return userRepository.save(User.builder()
                .email(normalized)
                .password(passwordEncoder.encode(password))
                .role(role)
                .build());
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
