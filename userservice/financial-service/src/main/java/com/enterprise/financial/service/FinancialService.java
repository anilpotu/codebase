package com.enterprise.financial.service;

import com.enterprise.financial.client.UserGrpcServiceClient;
import com.enterprise.financial.dto.*;
import com.enterprise.financial.entity.Account;
import com.enterprise.financial.entity.Transaction;
import com.enterprise.financial.repository.AccountRepository;
import com.enterprise.financial.repository.TransactionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FinancialService {

    private static final Logger log = LoggerFactory.getLogger(FinancialService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserGrpcServiceClient userGrpcServiceClient;

    public FinancialService(AccountRepository accountRepository,
                            TransactionRepository transactionRepository,
                            UserGrpcServiceClient userGrpcServiceClient) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userGrpcServiceClient = userGrpcServiceClient;
    }

    @Transactional
    @CircuitBreaker(name = "financialService")
    public AccountDTO createAccount(CreateAccountRequest request) {
        log.info("Creating account for userId={}, type={}", request.getUserId(), request.getAccountType());

        // Validate user exists in user-grpc-service before creating account
        try {
            ResponseEntity<GrpcUserDTO> userResponse = userGrpcServiceClient.getUserById(request.getUserId());
            if (!userResponse.getStatusCode().is2xxSuccessful() || userResponse.getBody() == null) {
                throw new RuntimeException("User not found with id: " + request.getUserId());
            }
            log.info("User validated: userId={}, name={}", request.getUserId(), userResponse.getBody().getName());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to validate user userId={}: {}", request.getUserId(), e.getMessage());
            throw new RuntimeException("User validation failed: " + e.getMessage());
        }

        Account account = new Account();
        account.setUserId(request.getUserId());
        account.setAccountType(request.getAccountType());
        account.setAccountNumber(UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        account.setBalance(BigDecimal.ZERO);
        account.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");

        Account saved = accountRepository.save(account);
        log.info("Account created: id={}, accountNumber={}", saved.getId(), saved.getAccountNumber());
        return toAccountDTO(saved);
    }

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "financialService")
    public List<AccountDTO> getAccountsByUserId(Long userId) {
        log.debug("Fetching accounts for userId={}", userId);
        return accountRepository.findByUserId(userId).stream()
                .map(this::toAccountDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "financialService")
    public AccountDTO getAccountById(Long id) {
        log.debug("Fetching account by id={}", id);
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Account not found: id={}", id);
                    return new RuntimeException("Account not found with id: " + id);
                });
        return toAccountDTO(account);
    }

    @Transactional
    @CircuitBreaker(name = "financialService")
    public TransactionDTO createTransaction(CreateTransactionRequest request) {
        log.info("Creating transaction: accountId={}, type={}, amount={}", request.getAccountId(), request.getTransactionType(), request.getAmount());
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> {
                    log.warn("Account not found for transaction: accountId={}", request.getAccountId());
                    return new RuntimeException("Account not found with id: " + request.getAccountId());
                });

        String type = request.getTransactionType().toUpperCase();
        BigDecimal amount = request.getAmount();

        if ("DEPOSIT".equals(type)) {
            account.setBalance(account.getBalance().add(amount));
            log.debug("Deposit applied: accountId={}, newBalance={}", account.getId(), account.getBalance());
        } else if ("WITHDRAWAL".equals(type)) {
            if (account.getBalance().compareTo(amount) < 0) {
                log.warn("Insufficient funds for withdrawal: accountId={}, balance={}, requested={}", account.getId(), account.getBalance(), amount);
                throw new RuntimeException("Insufficient funds. Current balance: " + account.getBalance());
            }
            account.setBalance(account.getBalance().subtract(amount));
            log.debug("Withdrawal applied: accountId={}, newBalance={}", account.getId(), account.getBalance());
        } else if ("TRANSFER".equals(type)) {
            if (account.getBalance().compareTo(amount) < 0) {
                log.warn("Insufficient funds for transfer: accountId={}, balance={}, requested={}", account.getId(), account.getBalance(), amount);
                throw new RuntimeException("Insufficient funds. Current balance: " + account.getBalance());
            }
            account.setBalance(account.getBalance().subtract(amount));
            log.debug("Transfer applied: accountId={}, newBalance={}", account.getId(), account.getBalance());
        }

        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(LocalDateTime.now());

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction created: id={}, accountId={}, type={}, amount={}", saved.getId(), account.getId(), type, amount);
        return toTransactionDTO(saved);
    }

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "financialService")
    public List<TransactionDTO> getTransactionsByAccountId(Long accountId) {
        log.debug("Fetching transactions for accountId={}", accountId);
        return transactionRepository.findByAccountIdOrderByTransactionDateDesc(accountId).stream()
                .map(this::toTransactionDTO)
                .collect(Collectors.toList());
    }

    private AccountDTO toAccountDTO(Account account) {
        return new AccountDTO(
                account.getId(),
                account.getUserId(),
                account.getAccountType(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getCurrency()
        );
    }

    private TransactionDTO toTransactionDTO(Transaction transaction) {
        return new TransactionDTO(
                transaction.getId(),
                transaction.getAccount().getId(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getTransactionDate()
        );
    }
}
