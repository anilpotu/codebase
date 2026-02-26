package com.enterprise.financial.service;

import com.enterprise.financial.dto.AccountDTO;
import com.enterprise.financial.dto.CreateAccountRequest;
import com.enterprise.financial.dto.CreateTransactionRequest;
import com.enterprise.financial.dto.TransactionDTO;
import com.enterprise.financial.entity.Account;
import com.enterprise.financial.entity.Transaction;
import com.enterprise.financial.repository.AccountRepository;
import com.enterprise.financial.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private FinancialService financialService;

    private Account sampleAccount;

    @BeforeEach
    void setUp() {
        sampleAccount = new Account();
        sampleAccount.setId(1L);
        sampleAccount.setUserId(100L);
        sampleAccount.setAccountType("SAVINGS");
        sampleAccount.setAccountNumber("ABC123DEF456GH78");
        sampleAccount.setBalance(new BigDecimal("1000.00"));
        sampleAccount.setCurrency("USD");
    }

    @Test
    void createAccount_success() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setUserId(100L);
        request.setAccountType("SAVINGS");
        request.setCurrency("USD");

        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        AccountDTO result = financialService.createAccount(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(100L, result.getUserId());
        assertEquals("SAVINGS", result.getAccountType());
        assertNotNull(result.getAccountNumber());
        assertEquals(16, result.getAccountNumber().length());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        assertEquals("USD", result.getCurrency());

        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void createAccount_nullCurrency_defaultsToUSD() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setUserId(100L);
        request.setAccountType("CHECKING");
        request.setCurrency(null);

        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        AccountDTO result = financialService.createAccount(request);

        assertEquals("USD", result.getCurrency());
    }

    @Test
    void getAccountsByUserId_returnsList() {
        Account account2 = new Account();
        account2.setId(2L);
        account2.setUserId(100L);
        account2.setAccountType("CHECKING");
        account2.setAccountNumber("XYZ987UVW654RS21");
        account2.setBalance(new BigDecimal("500.00"));
        account2.setCurrency("USD");

        when(accountRepository.findByUserId(100L)).thenReturn(Arrays.asList(sampleAccount, account2));

        List<AccountDTO> results = financialService.getAccountsByUserId(100L);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(1L, results.get(0).getId());
        assertEquals(2L, results.get(1).getId());

        verify(accountRepository, times(1)).findByUserId(100L);
    }

    @Test
    void getAccountsByUserId_noAccounts_returnsEmptyList() {
        when(accountRepository.findByUserId(999L)).thenReturn(Collections.emptyList());

        List<AccountDTO> results = financialService.getAccountsByUserId(999L);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void getAccountById_found() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount));

        AccountDTO result = financialService.getAccountById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(100L, result.getUserId());
        assertEquals("SAVINGS", result.getAccountType());
        assertEquals("ABC123DEF456GH78", result.getAccountNumber());
        assertEquals(new BigDecimal("1000.00"), result.getBalance());
        assertEquals("USD", result.getCurrency());

        verify(accountRepository, times(1)).findById(1L);
    }

    @Test
    void getAccountById_notFound_throwsException() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                financialService.getAccountById(999L)
        );

        assertTrue(exception.getMessage().contains("Account not found"));
        assertTrue(exception.getMessage().contains("999"));
    }

    @Test
    void createTransaction_deposit_increasesBalance() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAccountId(1L);
        request.setTransactionType("DEPOSIT");
        request.setAmount(new BigDecimal("250.00"));
        request.setDescription("Test deposit");

        TransactionDTO result = financialService.createTransaction(request);

        assertNotNull(result);
        assertEquals("DEPOSIT", result.getTransactionType());
        assertEquals(new BigDecimal("250.00"), result.getAmount());
        assertEquals("Test deposit", result.getDescription());
        // Balance should have been updated: 1000 + 250 = 1250
        assertEquals(new BigDecimal("1250.00"), sampleAccount.getBalance());

        verify(accountRepository, times(1)).save(sampleAccount);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void createTransaction_withdrawal_decreasesBalance() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAccountId(1L);
        request.setTransactionType("WITHDRAWAL");
        request.setAmount(new BigDecimal("300.00"));
        request.setDescription("Test withdrawal");

        TransactionDTO result = financialService.createTransaction(request);

        assertNotNull(result);
        assertEquals("WITHDRAWAL", result.getTransactionType());
        assertEquals(new BigDecimal("300.00"), result.getAmount());
        // Balance should have been updated: 1000 - 300 = 700
        assertEquals(new BigDecimal("700.00"), sampleAccount.getBalance());

        verify(accountRepository, times(1)).save(sampleAccount);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void createTransaction_withdrawal_insufficientFunds_throwsException() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount));

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAccountId(1L);
        request.setTransactionType("WITHDRAWAL");
        request.setAmount(new BigDecimal("5000.00"));
        request.setDescription("Too large withdrawal");

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                financialService.createTransaction(request)
        );

        assertTrue(exception.getMessage().contains("Insufficient funds"));
        // Balance should remain unchanged
        assertEquals(new BigDecimal("1000.00"), sampleAccount.getBalance());

        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_accountNotFound_throwsException() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAccountId(999L);
        request.setTransactionType("DEPOSIT");
        request.setAmount(new BigDecimal("100.00"));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                financialService.createTransaction(request)
        );

        assertTrue(exception.getMessage().contains("Account not found"));
    }

    @Test
    void getTransactionsByAccountId_returnsList() {
        Transaction tx1 = new Transaction();
        tx1.setId(1L);
        tx1.setAccount(sampleAccount);
        tx1.setTransactionType("DEPOSIT");
        tx1.setAmount(new BigDecimal("500.00"));
        tx1.setDescription("First deposit");
        tx1.setTransactionDate(LocalDateTime.of(2026, 1, 15, 10, 0));

        Transaction tx2 = new Transaction();
        tx2.setId(2L);
        tx2.setAccount(sampleAccount);
        tx2.setTransactionType("WITHDRAWAL");
        tx2.setAmount(new BigDecimal("100.00"));
        tx2.setDescription("First withdrawal");
        tx2.setTransactionDate(LocalDateTime.of(2026, 1, 16, 14, 30));

        when(transactionRepository.findByAccountIdOrderByTransactionDateDesc(1L))
                .thenReturn(Arrays.asList(tx2, tx1));

        List<TransactionDTO> results = financialService.getTransactionsByAccountId(1L);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("WITHDRAWAL", results.get(0).getTransactionType());
        assertEquals("DEPOSIT", results.get(1).getTransactionType());

        verify(transactionRepository, times(1)).findByAccountIdOrderByTransactionDateDesc(1L);
    }
}
