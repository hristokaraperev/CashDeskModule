package com.fibank.cashdesk.service.handler;

import com.fibank.cashdesk.exception.InsufficientFundsException;
import com.fibank.cashdesk.model.CashBalance;
import com.fibank.cashdesk.model.Currency;
import com.fibank.cashdesk.model.OperationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Handler for withdrawal operations.
 */
@Component
public class WithdrawalOperationHandler implements CashOperationHandler {

    private static final Logger log = LoggerFactory.getLogger(WithdrawalOperationHandler.class);

    @Override
    public OperationType getOperationType() {
        return OperationType.WITHDRAWAL;
    }

    @Override
    public void handle(CashBalance balance, Currency currency, BigDecimal amount, Map<Integer, Integer> denominations) {
        // Validate denominations sum to amount
        CashBalance.validateDenominationSum(denominations, amount);

        // Validate denominations are valid for currency
        currency.validateDenominations(denominations);

        // Check if sufficient denominations are available
        if (!balance.hasSufficientDenominations(denominations)) {
            BigDecimal currentTotal = balance.calculateTotal();
            throw new InsufficientFundsException(
                String.format("Insufficient funds for withdrawal. Requested: %s %s, Available: %s %s",
                    amount, currency, currentTotal, currency)
            );
        }

        // Remove denominations from balance
        balance.removeDenominations(denominations);

        log.debug("Withdrawal processed: {} {} with denominations {}", amount, currency, denominations);
    }
}
