package com.enterprise.financial.controller;

import com.enterprise.financial.dto.AccountDTO;
import com.enterprise.financial.dto.CreateAccountRequest;
import com.enterprise.financial.service.FinancialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    private final FinancialService financialService;

    public AccountController(FinancialService financialService) {
        this.financialService = financialService;
    }

    @PostMapping
    public ResponseEntity<AccountDTO> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        log.info("POST /api/accounts - Creating account for userId={}", request.getUserId());
        AccountDTO account = financialService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccountDTO>> getAccountsByUserId(@PathVariable Long userId) {
        log.debug("GET /api/accounts/user/{}", userId);
        List<AccountDTO> accounts = financialService.getAccountsByUserId(userId);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDTO> getAccountById(@PathVariable Long id) {
        log.debug("GET /api/accounts/{}", id);
        AccountDTO account = financialService.getAccountById(id);
        return ResponseEntity.ok(account);
    }
}
