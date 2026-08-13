package com.example.demo.controller;

import com.example.demo.dto.request.EmartCardApplicationRequest;
import com.example.demo.dto.response.EmartCardResponse;
import com.example.demo.dto.response.PointsBalanceResponse;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.interfaces.EmartCardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Module 4 — e-MART card. Both endpoints require a logged-in user. */
@RestController
@RequestMapping("/api/emart-card")
public class EmartCardController {
    public EmartCardController(EmartCardService emartCardService) {
        this.emartCardService = emartCardService;
    }


    private final EmartCardService emartCardService;

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<EmartCardResponse>> apply(
            @Valid @RequestBody EmartCardApplicationRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "e-MART card application submitted. It is now pending review.",
                emartCardService.apply(request)));
    }

    /**
     * GET /api/emart-card/balance
     *
     * Backs the "Redeem e-Points on this item" checkbox. Unlike /me it never
     * 404s — a non-cardholder gets cardholder=false and pointsBalance=0, so the
     * UI can decide what to show without treating "no card" as an error.
     */
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<PointsBalanceResponse>> getMyPointsBalance() {
        return ResponseEntity.ok(ApiResponse.success(
                "Points balance retrieved successfully", emartCardService.getMyPointsBalance()));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<EmartCardResponse>> getMyCard() {
        return ResponseEntity.ok(ApiResponse.success(
                "Card retrieved successfully", emartCardService.getMyCard()));
    }
}
