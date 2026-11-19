package com.hampcode.ticketsales.mapper;

import com.hampcode.ticketsales.dto.response.PaymentResponseDTO;
import com.hampcode.ticketsales.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    @Mapping(target = "ticketId", source = "ticket.id")
    PaymentResponseDTO toResponseDTO(Payment payment);
}
