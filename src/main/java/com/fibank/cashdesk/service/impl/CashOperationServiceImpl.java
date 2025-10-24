package com.fibank.cashdesk.service.impl;

import com.fibank.cashdesk.dto.request.CashOperationRequest;
import com.fibank.cashdesk.dto.response.CashOperationResponse;
import com.fibank.cashdesk.exception.InvalidCashierException;
import com.fibank.cashdesk.model.*;
import com.fibank.cashdesk.repository.BalanceRepository;
import com.fibank.cashdesk.repository.TransactionRepository;
import com.fibank.cashdesk.service.CashOperationService;
import com.fibank.cashdesk.service.handler.CashOperationHandler;
import com.fibank.cashdesk.util.MdcUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of CashOperationService.
 * Coordinates transaction processing with atomicity and rollback support.
 */
@Service
public class CashOperationServiceImpl implements CashOperationService {

    private static final Logger log = LoggerFactory.getLogger(CashOperationServiceImpl.class);

    private final TransactionRepository transactionRepository;
    private final BalanceRepository balanceRepository;
    private final Map<OperationType, CashOperationHandler> handlers;

    public CashOperationServiceImpl(
        TransactionRepository transactionRepository,
        BalanceRepository balanceRepository,
        List<CashOperationHandler> handlerList
    ) {
        this.transactionRepository = transactionRepository;
        this.balanceRepository = balanceRepository;
        this.handlers = handlerList.stream()
            .collect(Collectors.toMap(
                CashOperationHandler::getOperationType,
                handler -> handler
            ));
    }

    @Override
    public CashOperationResponse processOperation(CashOperationRequest request) {
        String cashierName = request.getCashier().toUpperCase();
        if (!Cashier.isValid(cashierName)) {
            throw new InvalidCashierException("Invalid cashier: " + cashierName);
        }

        OperationType operationType = OperationType.fromString(request.getOperationType());
        Currency currency = Currency.valueOf(request.getCurrency().toUpperCase());

        MdcUtil.setCashier(cashierName);
        MdcUtil.setOperationType(operationType.name());
        MdcUtil.setCurrency(currency.name());
        MdcUtil.setAmount(request.getAmount().toPlainString());

        CashOperationHandler handler = handlers.get(operationType);
        if (handler == null) {
            throw new IllegalStateException("No handler found for operation type: " + operationType);
        }

        Map<Currency, CashBalance> cashierBalances = balanceRepository.findByCashier(cashierName);
        CashBalance balance = cashierBalances.get(currency);

        if (balance == null) {
            balance = new CashBalance(currency);
            cashierBalances.put(currency, balance);
        }

        Map<Integer, Integer> originalDenominations = balance.getDenominations();

        try {
            handler.handle(balance, currency, request.getAmount(), request.getDenominations());

            Transaction transaction = Transaction.create(
                cashierName,
                operationType,
                currency,
                request.getAmount(),
                request.getDenominations()
            );

            MdcUtil.setTransactionId(transaction.getId());

            balanceRepository.save(cashierName, cashierBalances);
            transactionRepository.save(transaction);

            log.info("Cash operation completed successfully: {} {} {} with denominations {}",
                operationType == OperationType.DEPOSIT ? "deposited" : "withdrew",
                request.getAmount(),
                currency,
                formatDenominations(request.getDenominations())
            );

            return new CashOperationResponse(
                transaction.getId().toString(),
                transaction.getTimestamp(),
                transaction.getCashier(),
                transaction.getOperationType().name(),
                transaction.getCurrency().name(),
                transaction.getAmount(),
                transaction.getDenominations(),
                String.format("%s successful", operationType)
            );

        } catch (Exception e) {
            log.error("Operation failed, rolling back balance", e);

            for (Map.Entry<Integer, Integer> entry : originalDenominations.entrySet()) {
                balance.setDenominationCount(entry.getKey(), entry.getValue());
            }

            throw e;
        }
    }

    private String formatDenominations(Map<Integer, Integer> denominations) {
        return denominations.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getValue() + "x" + e.getKey())
            .collect(Collectors.joining(", "));
    }
}
