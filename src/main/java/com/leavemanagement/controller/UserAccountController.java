package com.leavemanagement.controller;

import com.leavemanagement.dto.UserAccountRequest;
import com.leavemanagement.dto.UserAccountResponse;
import com.leavemanagement.service.UserAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userAccountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserAccountResponse createAccount(@Valid @RequestBody UserAccountRequest request) {
        return userAccountService.createAccount(request);
    }

    @GetMapping
    public List<UserAccountResponse> getAllAccounts() {
        return userAccountService.getAllAccounts();
    }

    @GetMapping("/{id}")
    public UserAccountResponse getAccountById(@PathVariable Long id) {
        return userAccountService.getAccountById(id);
    }

    @PutMapping("/{id}/deactivate")
    public UserAccountResponse deactivate(@PathVariable Long id) {
        return userAccountService.deactivate(id);
    }

    @PutMapping("/{id}/activate")
    public UserAccountResponse activate(@PathVariable Long id) {
        return userAccountService.activate(id);
    }
}
