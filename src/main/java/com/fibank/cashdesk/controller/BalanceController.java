package com.fibank.cashdesk.controller;

import com.fibank.cashdesk.dto.response.BalanceQueryResponse;
import com.fibank.cashdesk.service.BalanceQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * REST controller for balance queries.
 */
@RestController
@RequestMapping("/api/v1")
public class BalanceController {

    private static final Logger log = LoggerFactory.getLogger(BalanceController.class);

    private final BalanceQueryService balanceQueryService;

    public BalanceController(BalanceQueryService balanceQueryService) {
        this.balanceQueryService = balanceQueryService;
    }

    /**
     * Query cash balances with optional filters.
     *
     * @param dateFrom Start date (optional)
     * @param dateTo End date (optional)
     * @param cashier Cashier name (optional)
     * @return Balance query response
     */
    @GetMapping("/cash-balance")
    public ResponseEntity<BalanceQueryResponse> queryBalance(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
        @RequestParam(required = false) String cashier
    ) {
        log.info("Balance query - cashier: {}, dateFrom: {}, dateTo: {}", cashier, dateFrom, dateTo);

        BalanceQueryResponse response = balanceQueryService.queryBalance(dateFrom, dateTo, cashier);

        return ResponseEntity.ok(response);
    }
}
