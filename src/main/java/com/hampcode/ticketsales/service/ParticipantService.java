package com.hampcode.ticketsales.service;

import com.hampcode.ticketsales.config.CurrentUser;
import com.hampcode.ticketsales.dto.request.ParticipantRequestDTO;
import com.hampcode.ticketsales.dto.request.ParticipantUpdateRequestDTO;
import com.hampcode.ticketsales.dto.response.PageResponseDTO;
import com.hampcode.ticketsales.dto.response.ParticipantResponseDTO;
import com.hampcode.ticketsales.dto.response.TicketResponseDTO;
import com.hampcode.ticketsales.entity.Participant;
import com.hampcode.ticketsales.entity.User;
import com.hampcode.ticketsales.entity.Ticket;
import com.hampcode.ticketsales.exception.DuplicateResourceException;
import com.hampcode.ticketsales.exception.ResourceNotFoundException;
import com.hampcode.ticketsales.mapper.ParticipantMapper;
import com.hampcode.ticketsales.mapper.TicketMapper;
import com.hampcode.ticketsales.repository.ParticipantRepository;
import com.hampcode.ticketsales.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipantService {
    private final ParticipantRepository participantRepository;
    private final TicketRepository ticketRepository;
    private final ParticipantMapper participantMapper;
    private final TicketMapper ticketMapper;

    @Transactional
    public ParticipantResponseDTO registerParticipant(ParticipantRequestDTO request, User user) {
        if (participantRepository.findByDocumentIdAndName(request.documentId(), request.name()).isPresent()) {
            throw new DuplicateResourceException("El participante ya está registrado");
        }
        Participant participant = participantMapper.toEntity(request);
        participant.setUser(user);
        Participant saved = participantRepository.save(participant);
        return participantMapper.toResponseDTO(saved);
    }

    @Transactional
    public ParticipantResponseDTO updateContactInfo(Long participantId, ParticipantUpdateRequestDTO request) {
        Participant current = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participante no encontrado"));

        if (!current.getUser().getId().equals(CurrentUser.getUserId())) {
            throw new AccessDeniedException("No es tu información");
        }
        String email = request.email() != null ? request.email() : current.getEmail();
        String phone = request.phone() != null ? request.phone() : current.getPhone();
        participantRepository.updateContactInfo(participantId, email, phone);
        return participantMapper.toResponseDTO(participantRepository.findById(participantId).orElseThrow());
    }

    public PageResponseDTO<TicketResponseDTO> listTicketsByParticipant(Long participantId, Pageable pageable) {
        requireParticipant(participantId);
        Page<Ticket> page = ticketRepository.findByParticipantId(participantId, pageable);
        return new PageResponseDTO<>(
                page.getContent().stream().map(ticketMapper::toResponseDTO).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isLast());
    }

    public List<TicketResponseDTO> listTicketsByParticipantAndEvent(Long participantId, Long eventId) {
        requireParticipant(participantId);
        return ticketRepository.findByParticipantIdAndEventId(participantId, eventId).stream()
                .map(ticketMapper::toResponseDTO)
                .toList();
    }

    private void requireParticipant(Long participantId) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participante no encontrado"));

        if (!participant.getUser().getId().equals(CurrentUser.getUserId())) {
            throw new AccessDeniedException("No es tu información");
        }
    }
}
