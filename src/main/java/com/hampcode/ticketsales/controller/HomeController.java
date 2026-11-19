package com.hampcode.ticketsales.controller;

import com.hampcode.ticketsales.dto.response.HomeResponseDTO;
import com.hampcode.ticketsales.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeController {
    private final HomeService homeService;

    @GetMapping
    public ResponseEntity<HomeResponseDTO> getHome() {
        return ResponseEntity.status(HttpStatus.OK).body(homeService.getHome());
    }
}
