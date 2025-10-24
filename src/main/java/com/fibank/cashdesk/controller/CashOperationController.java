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
     * Requires idempotency through the mandatory Idempotency-Key header.
     * If a request with the same idempotency key has been processed before,
     * the cached response will be returned instead of processing again.
     * This is a critical security requirement for banking operations to prevent
     * duplicate transactions due to network retries or accidental resubmissions.
     *
     * @param request Cash operation request
     * @param idempotencyKey Mandatory idempotency key (UUID format) for duplicate prevention
     * @return Cash operation response
     * @throws com.fibank.cashdesk.exception.InvalidIdempotencyKeyException if key is missing, blank, or invalid UUID
     */
    @PostMapping("/cash-operation")
    public ResponseEntity<CashOperationResponse> processCashOperation(
        @Valid @RequestBody CashOperationRequest request,
        @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey
    ) {
        // Validate idempotency key
        validateIdempotencyKey(idempotencyKey);

        log.info("Received {} request for cashier {}: {} {} (Idempotency-Key: {})",
            request.getOperationType(),
            request.getCashier(),
            request.getAmount(),
            request.getCurrency(),
            idempotencyKey
        );

        // Check for cached response
        Optional<CashOperationResponse> cachedResponse = idempotencyService.getCachedResponse(idempotencyKey);

        if (cachedResponse.isPresent()) {
            log.info("Duplicate request detected with idempotency key: {}. Returning cached response.",
                    idempotencyKey);
            return ResponseEntity.status(HttpStatus.OK).body(cachedResponse.get());
        }

        // Process the operation
        CashOperationResponse response = cashOperationService.processOperation(request);

        // Cache the response
        idempotencyService.cacheResponse(idempotencyKey, response);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Validates that the idempotency key is present, not blank, and follows UUID format.
     *
     * @param idempotencyKey The idempotency key to validate
     * @throws com.fibank.cashdesk.exception.InvalidIdempotencyKeyException if validation fails
     */
    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new com.fibank.cashdesk.exception.InvalidIdempotencyKeyException(
                "Idempotency-Key header is required for all cash operations");
        }

        // Validate UUID format
        String uuidPattern = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
        if (!idempotencyKey.matches(uuidPattern)) {
            throw new com.fibank.cashdesk.exception.InvalidIdempotencyKeyException(
                "Idempotency-Key must be a valid UUID format (e.g., 550e8400-e29b-41d4-a716-446655440000)");
        }
    }
}
