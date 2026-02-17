package com.enterprise.financial.controller;

import com.enterprise.financial.dto.CreateTransactionRequest;
import com.enterprise.financial.entity.Account;
import com.enterprise.financial.repository.AccountRepository;
import com.enterprise.financial.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();

        Account account = new Account();
        account.setUserId(100L);
        account.setAccountType("SAVINGS");
        account.setAccountNumber("TXNTEST000000001");
        account.setBalance(new BigDecimal("1000.00"));
        account.setCurrency("USD");
        testAccount = accountRepository.save(account);
    }

    @AfterEach
    void tearDown() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void createTransaction_deposit_returns201() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAccountId(testAccount.getId());
        request.setTransactionType("DEPOSIT");
        request.setAmount(new BigDecimal("250.00"));
        request.setDescription("Salary deposit");

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.accountId", is(testAccount.getId().intValue())))
                .andExpect(jsonPath("$.transactionType", is("DEPOSIT")))
                .andExpect(jsonPath("$.amount", comparesEqualTo(250.00)))
                .andExpect(jsonPath("$.description", is("Salary deposit")))
                .andExpect(jsonPath("$.transactionDate", notNullValue()));

        // Verify balance was updated
        Account updated = accountRepository.findById(testAccount.getId()).orElseThrow(() -> new RuntimeException("not found"));
        assert updated.getBalance().compareTo(new BigDecimal("1250.00")) == 0;
    }

    @Test
    void createTransaction_withdrawal_returns201() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAccountId(testAccount.getId());
        request.setTransactionType("WITHDRAWAL");
        request.setAmount(new BigDecimal("300.00"));
        request.setDescription("ATM withdrawal");

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType", is("WITHDRAWAL")))
                .andExpect(jsonPath("$.amount", comparesEqualTo(300.00)));

        // Verify balance was updated
        Account updated = accountRepository.findById(testAccount.getId()).orElseThrow(() -> new RuntimeException("not found"));
        assert updated.getBalance().compareTo(new BigDecimal("700.00")) == 0;
    }

    @Test
    void createTransaction_withdrawal_insufficientFunds_throwsException() {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAccountId(testAccount.getId());
        request.setTransactionType("WITHDRAWAL");
        request.setAmount(new BigDecimal("5000.00"));
        request.setDescription("Too large");

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
            mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
        );

        // Verify balance was NOT changed
        Account updated = accountRepository.findById(testAccount.getId()).orElseThrow(() -> new RuntimeException("not found"));
        assert updated.getBalance().compareTo(new BigDecimal("1000.00")) == 0;
    }

    @Test
    void getTransactionsByAccountId_returns200() throws Exception {
        // Create two transactions via the API
        CreateTransactionRequest deposit = new CreateTransactionRequest();
        deposit.setAccountId(testAccount.getId());
        deposit.setTransactionType("DEPOSIT");
        deposit.setAmount(new BigDecimal("100.00"));
        deposit.setDescription("First deposit");

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deposit)))
                .andExpect(status().isCreated());

        CreateTransactionRequest withdrawal = new CreateTransactionRequest();
        withdrawal.setAccountId(testAccount.getId());
        withdrawal.setTransactionType("WITHDRAWAL");
        withdrawal.setAmount(new BigDecimal("50.00"));
        withdrawal.setDescription("First withdrawal");

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawal)))
                .andExpect(status().isCreated());

        // Fetch transactions
        mockMvc.perform(get("/api/transactions/account/" + testAccount.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getTransactionsByAccountId_noTransactions_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/transactions/account/" + testAccount.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
