package com.fibank.cashdesk.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO representing a summary for a specific currency within a period.
 * Contains starting balance, ending balance, net change, and transaction list.
 */
public class PeriodSummaryDTO {
    private String currency;
    private BigDecimal startingTotal;
    private Map<Integer, Integer> startingDenominations;
    private BigDecimal endingTotal;
    private Map<Integer, Integer> endingDenominations;
    private BigDecimal netChange;
    private List<TransactionDTO> transactions;

    public PeriodSummaryDTO() {
    }

    public PeriodSummaryDTO(
        String currency,
        BigDecimal startingTotal,
        Map<Integer, Integer> startingDenominations,
        BigDecimal endingTotal,
        Map<Integer, Integer> endingDenominations,
        BigDecimal netChange,
        List<TransactionDTO> transactions
    ) {
        this.currency = currency;
        this.startingTotal = startingTotal;
        this.startingDenominations = startingDenominations;
        this.endingTotal = endingTotal;
        this.endingDenominations = endingDenominations;
        this.netChange = netChange;
        this.transactions = transactions;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getStartingTotal() {
        return startingTotal;
    }

    public void setStartingTotal(BigDecimal startingTotal) {
        this.startingTotal = startingTotal;
    }

    public Map<Integer, Integer> getStartingDenominations() {
        return startingDenominations;
    }

    public void setStartingDenominations(Map<Integer, Integer> startingDenominations) {
        this.startingDenominations = startingDenominations;
    }

    public BigDecimal getEndingTotal() {
        return endingTotal;
    }

    public void setEndingTotal(BigDecimal endingTotal) {
        this.endingTotal = endingTotal;
    }

    public Map<Integer, Integer> getEndingDenominations() {
        return endingDenominations;
    }

    public void setEndingDenominations(Map<Integer, Integer> endingDenominations) {
        this.endingDenominations = endingDenominations;
    }

    public BigDecimal getNetChange() {
        return netChange;
    }

    public void setNetChange(BigDecimal netChange) {
        this.netChange = netChange;
    }

    public List<TransactionDTO> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionDTO> transactions) {
        this.transactions = transactions;
    }
}
