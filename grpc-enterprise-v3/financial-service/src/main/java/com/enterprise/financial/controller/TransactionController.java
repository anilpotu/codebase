package com.enterprise.financial.controller;

import com.enterprise.financial.dto.CreateTransactionRequest;
import com.enterprise.financial.dto.TransactionDTO;
import com.enterprise.financial.service.FinancialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final FinancialService financialService;

    public TransactionController(FinancialService financialService) {
        this.financialService = financialService;
    }

    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        log.info("POST /api/transactions - accountId={}, type={}, amount={}", request.getAccountId(), request.getTransactionType(), request.getAmount());
        TransactionDTO transaction = financialService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByAccountId(@PathVariable Long accountId) {
        log.debug("GET /api/transactions/account/{}", accountId);
        List<TransactionDTO> transactions = financialService.getTransactionsByAccountId(accountId);
        return ResponseEntity.ok(transactions);
    }
}
