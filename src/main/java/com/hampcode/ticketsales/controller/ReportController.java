package com.hampcode.ticketsales.controller;

import com.hampcode.ticketsales.dto.response.DashboardDTO;
import com.hampcode.ticketsales.dto.response.PageResponseDTO;
import com.hampcode.ticketsales.dto.response.TicketHistoryResponseDTO;
import com.hampcode.ticketsales.dto.response.TicketReportResponseDTO;
import com.hampcode.ticketsales.dto.response.TotalSpentResponseDTO;
import com.hampcode.ticketsales.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @PreAuthorize("hasRole('ORGANIZER')")
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDTO> getDashboard() {
        return ResponseEntity.ok(reportService.getMyDashboard());
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @GetMapping("/events/{eventId}")
    public ResponseEntity<TicketReportResponseDTO> getEventSalesReport(@PathVariable Long eventId) {
        return ResponseEntity.status(HttpStatus.OK).body(reportService.getEventSalesReport(eventId));
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @GetMapping("/events/{eventId}/full")
    public ResponseEntity<TicketReportResponseDTO> getEventSalesReportFull(@PathVariable Long eventId) {
        return ResponseEntity.status(HttpStatus.OK).body(reportService.getEventSalesReportFull(eventId));
    }

    @PreAuthorize("hasRole('PARTICIPANT')")
    @GetMapping("/participants/{participantId}/history")
    public ResponseEntity<PageResponseDTO<TicketHistoryResponseDTO>> getPurchaseHistory(
            @PathVariable Long participantId, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(reportService.getPurchaseHistory(participantId, pageable));
    }

    @PreAuthorize("hasRole('PARTICIPANT')")
    @GetMapping("/participants/{participantId}/history/full")
    public ResponseEntity<List<TicketHistoryResponseDTO>> getPurchaseHistoryFull(@PathVariable Long participantId) {
        return ResponseEntity.status(HttpStatus.OK).body(reportService.getPurchaseHistoryFull(participantId));
    }

    @PreAuthorize("hasRole('PARTICIPANT')")
    @GetMapping("/participants/{participantId}/total-spent")
    public ResponseEntity<TotalSpentResponseDTO> getTotalSpent(@PathVariable Long participantId) {
        return ResponseEntity.status(HttpStatus.OK).body(reportService.getTotalSpent(participantId));
    }
}
