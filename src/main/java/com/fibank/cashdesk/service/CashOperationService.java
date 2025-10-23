package com.fibank.cashdesk.service;

import com.fibank.cashdesk.dto.request.CashOperationRequest;
import com.fibank.cashdesk.dto.response.CashOperationResponse;

/**
 * Service interface for processing cash operations.
 */
public interface CashOperationService {

    /**
     * Process a cash operation (deposit or withdrawal).
     * @param request Operation request
     * @return Operation response with transaction details
     */
    CashOperationResponse processOperation(CashOperationRequest request);
}
