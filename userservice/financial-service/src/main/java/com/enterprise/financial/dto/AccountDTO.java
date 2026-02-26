package com.enterprise.financial.dto;

import java.math.BigDecimal;

public class AccountDTO {

    private Long id;
    private Long userId;
    private String accountType;
    private String accountNumber;
    private BigDecimal balance;
    private String currency;

    public AccountDTO() {
    }

    public AccountDTO(Long id, Long userId, String accountType, String accountNumber,
                      BigDecimal balance, String currency) {
        this.id = id;
        this.userId = userId;
        this.accountType = accountType;
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.currency = currency;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
