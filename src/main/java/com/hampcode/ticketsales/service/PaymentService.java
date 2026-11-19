package com.hampcode.ticketsales.service;

import com.hampcode.ticketsales.config.CurrentUser;
import com.hampcode.ticketsales.dto.request.PaymentRequestDTO;
import com.hampcode.ticketsales.dto.response.PaymentResponseDTO;
import com.hampcode.ticketsales.entity.Payment;
import com.hampcode.ticketsales.entity.Ticket;
import com.hampcode.ticketsales.exception.DuplicateResourceException;
import com.hampcode.ticketsales.exception.ResourceNotFoundException;
import com.hampcode.ticketsales.mapper.PaymentMapper;
import com.hampcode.ticketsales.repository.PaymentRepository;
import com.hampcode.ticketsales.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final PaymentMapper paymentMapper;

    @Transactional
    public PaymentResponseDTO registerPayment(PaymentRequestDTO request) {
        Ticket ticket = ticketRepository.findById(request.ticketId())
                .orElseThrow(() -> new ResourceNotFoundException("Entrada no encontrada"));

        if (!ticket.getParticipant().getUser().getId().equals(CurrentUser.getUserId())) {
            throw new AccessDeniedException("No es tu entrada");
        }

        if (paymentRepository.findByTicketId(ticket.getId()).isPresent()) {
            throw new DuplicateResourceException("La entrada ya tiene un pago registrado");
        }

        Payment payment = Payment.builder()
                .ticket(ticket)
                .amount(request.amount())
                .method(request.method())
                .status("APPROVED")
                .build();
        Payment saved = paymentRepository.save(payment);

        ticket.setStatus("PAID");
        ticketRepository.save(ticket);
        return paymentMapper.toResponseDTO(saved);
    }
}
