package com.fibank.cashdesk.controller;

import com.fibank.cashdesk.dto.request.CashOperationRequest;
import com.fibank.cashdesk.dto.response.CashOperationResponse;
import com.fibank.cashdesk.service.CashOperationService;
import com.fibank.cashdesk.service.IdempotencyService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * REST controller for cash operations (deposits and withdrawals).
 */
@RestController
@RequestMapping("/api/v1")
public class CashOperationController {

    private static final Logger log = LoggerFactory.getLogger(CashOperationController.class);

    private final CashOperationService cashOperationService;
    private final IdempotencyService idempotencyService;

    public CashOperationController(CashOperationService cashOperationService,
                                    IdempotencyService idempotencyService) {
        this.cashOperationService = cashOperationService;
        this.idempotencyService = idempotencyService;
    }

    /**
     * Process a cash operation (deposit or withdrawal).
     * Supports idempotency through the optional Idempotency-Key header.
     * If a request with the same idempotency key has been processed before,
     * the cached response will be returned instead of processing again.
     *
     * @param request Cash operation request
     * @param idempotencyKey Optional idempotency key for duplicate prevention
     * @return Cash operation response
     */
    @PostMapping("/cash-operation")
    public ResponseEntity<CashOperationResponse> processCashOperation(
        @Valid @RequestBody CashOperationRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        log.info("Received {} request for cashier {}: {} {} (Idempotency-Key: {})",
            request.getOperationType(),
            request.getCashier(),
            request.getAmount(),
            request.getCurrency(),
            idempotencyKey != null ? idempotencyKey : "not provided"
        );

        // Check for cached response if idempotency key is provided
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<CashOperationResponse> cachedResponse = idempotencyService.getCachedResponse(idempotencyKey);

            if (cachedResponse.isPresent()) {
                log.info("Duplicate request detected with idempotency key: {}. Returning cached response.",
                        idempotencyKey);
                return ResponseEntity.status(HttpStatus.OK).body(cachedResponse.get());
            }
        }

        // Process the operation
        CashOperationResponse response = cashOperationService.processOperation(request);

        // Cache the response if idempotency key is provided
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.cacheResponse(idempotencyKey, response);
        }

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
