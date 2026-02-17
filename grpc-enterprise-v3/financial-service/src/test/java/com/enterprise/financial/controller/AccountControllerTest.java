package com.enterprise.financial.controller;

import com.enterprise.financial.dto.CreateAccountRequest;
import com.enterprise.financial.entity.Account;
import com.enterprise.financial.repository.AccountRepository;
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
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        accountRepository.deleteAll();
    }

    @Test
    void createAccount_returns201() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setUserId(100L);
        request.setAccountType("SAVINGS");
        request.setCurrency("USD");

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.userId", is(100)))
                .andExpect(jsonPath("$.accountType", is("SAVINGS")))
                .andExpect(jsonPath("$.accountNumber", notNullValue()))
                .andExpect(jsonPath("$.balance", comparesEqualTo(0)))
                .andExpect(jsonPath("$.currency", is("USD")));
    }

    @Test
    void createAccount_missingUserId_returns400() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountType("SAVINGS");

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAccountsByUserId_returns200() throws Exception {
        // Create two accounts for the same user
        Account account1 = new Account();
        account1.setUserId(200L);
        account1.setAccountType("SAVINGS");
        account1.setAccountNumber("ACCT000000000001");
        account1.setBalance(BigDecimal.ZERO);
        account1.setCurrency("USD");
        accountRepository.save(account1);

        Account account2 = new Account();
        account2.setUserId(200L);
        account2.setAccountType("CHECKING");
        account2.setAccountNumber("ACCT000000000002");
        account2.setBalance(new BigDecimal("500.00"));
        account2.setCurrency("USD");
        accountRepository.save(account2);

        mockMvc.perform(get("/api/accounts/user/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].userId", is(200)))
                .andExpect(jsonPath("$[1].userId", is(200)));
    }

    @Test
    void getAccountsByUserId_noAccounts_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/accounts/user/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAccountById_returns200() throws Exception {
        Account account = new Account();
        account.setUserId(300L);
        account.setAccountType("SAVINGS");
        account.setAccountNumber("ACCT000000000003");
        account.setBalance(new BigDecimal("750.00"));
        account.setCurrency("EUR");
        Account saved = accountRepository.save(account);

        mockMvc.perform(get("/api/accounts/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(saved.getId().intValue())))
                .andExpect(jsonPath("$.userId", is(300)))
                .andExpect(jsonPath("$.accountType", is("SAVINGS")))
                .andExpect(jsonPath("$.accountNumber", is("ACCT000000000003")))
                .andExpect(jsonPath("$.currency", is("EUR")));
    }

    @Test
    void getAccountById_notFound_throwsException() {
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
            mockMvc.perform(get("/api/accounts/999"))
        );
    }
}
