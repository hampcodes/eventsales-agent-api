package com.hampcode.ticketsales.service;

import com.hampcode.ticketsales.dto.request.OrganizerRequestDTO;
import com.hampcode.ticketsales.dto.response.OrganizerResponseDTO;
import com.hampcode.ticketsales.entity.Organizer;
import com.hampcode.ticketsales.entity.User;
import com.hampcode.ticketsales.exception.DuplicateResourceException;
import com.hampcode.ticketsales.mapper.OrganizerMapper;
import com.hampcode.ticketsales.repository.OrganizerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizerService {
    private final OrganizerRepository organizerRepository;
    private final OrganizerMapper organizerMapper;

    @Transactional
    public OrganizerResponseDTO registerOrganizer(OrganizerRequestDTO request, User user) {
        if (organizerRepository.findByDocumentIdAndName(request.documentId(), request.name()).isPresent()) {
            throw new DuplicateResourceException("El organizador ya está registrado");
        }
        Organizer organizer = organizerMapper.toEntity(request);
        organizer.setUser(user);
        Organizer saved = organizerRepository.save(organizer);
        return organizerMapper.toResponseDTO(saved);
    }
}
