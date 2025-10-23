package com.fibank.cashdesk.controller;

import com.fibank.cashdesk.dto.request.CashOperationRequest;
import com.fibank.cashdesk.dto.response.CashOperationResponse;
import com.fibank.cashdesk.service.CashOperationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for cash operations (deposits and withdrawals).
 */
@RestController
@RequestMapping("/api/v1")
public class CashOperationController {

    private static final Logger log = LoggerFactory.getLogger(CashOperationController.class);

    private final CashOperationService cashOperationService;

    public CashOperationController(CashOperationService cashOperationService) {
        this.cashOperationService = cashOperationService;
    }

    /**
     * Process a cash operation (deposit or withdrawal).
     *
     * @param request Cash operation request
     * @return Cash operation response
     */
    @PostMapping("/cash-operation")
    public ResponseEntity<CashOperationResponse> processCashOperation(
        @Valid @RequestBody CashOperationRequest request
    ) {
        log.info("Received {} request for cashier {}: {} {}",
            request.getOperationType(),
            request.getCashier(),
            request.getAmount(),
            request.getCurrency()
        );

        CashOperationResponse response = cashOperationService.processOperation(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
