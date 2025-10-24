package com.fibank.cashdesk.service.handler;

import com.fibank.cashdesk.model.CashBalance;
import com.fibank.cashdesk.model.Currency;
import com.fibank.cashdesk.model.OperationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Handler for deposit operations.
 */
@Component
public class DepositOperationHandler implements CashOperationHandler {

    private static final Logger log = LoggerFactory.getLogger(DepositOperationHandler.class);

    @Override
    public OperationType getOperationType() {
        return OperationType.DEPOSIT;
    }

    @Override
    public void handle(CashBalance balance, Currency currency, BigDecimal amount, Map<Integer, Integer> denominations) {
        CashBalance.validateDenominationSum(denominations, amount);
        currency.validateDenominations(denominations);
        balance.addDenominations(denominations);

        log.debug("Deposit processed: {} {} with denominations {}", amount, currency, denominations);
    }
}
