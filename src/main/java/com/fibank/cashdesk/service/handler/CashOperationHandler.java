package com.fibank.cashdesk.service.handler;

import com.fibank.cashdesk.model.CashBalance;
import com.fibank.cashdesk.model.Currency;
import com.fibank.cashdesk.model.OperationType;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Strategy interface for handling different cash operation types.
 */
public interface CashOperationHandler {

    /**
     * Get the operation type this handler supports.
     * @return Operation type
     */
    OperationType getOperationType();

    /**
     * Handle the cash operation.
     * @param balance Current cash balance
     * @param currency Operation currency
     * @param amount Operation amount
     * @param denominations Denominations involved
     */
    void handle(CashBalance balance, Currency currency, BigDecimal amount, Map<Integer, Integer> denominations);
}
