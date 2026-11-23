package com.leavemanagement.service;

import com.leavemanagement.dto.UserAccountRequest;
import com.leavemanagement.dto.UserAccountResponse;
import com.leavemanagement.entity.AccountStatus;
import com.leavemanagement.entity.Employee;
import com.leavemanagement.entity.Role;
import com.leavemanagement.entity.UserAccount;
import com.leavemanagement.exception.BusinessRuleException;
import com.leavemanagement.exception.DuplicateResourceException;
import com.leavemanagement.exception.ResourceNotFoundException;
import com.leavemanagement.repository.EmployeeRepository;
import com.leavemanagement.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository;

    // BCrypt = one-way hash. The plain password is never stored.
    // (In Step 10, when Spring Security is added, this becomes a shared bean.)
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public UserAccountResponse createAccount(UserAccountRequest request) {
        String username = request.username().trim();

        if (userAccountRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateResourceException("Username '" + username + "' is already taken");
        }

        Employee employee = null;
        if (request.employeeId() != null) {
            employee = employeeRepository.findById(request.employeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + request.employeeId()));

            // Rule: one account per employee
            if (userAccountRepository.existsByEmployeeEmpId(employee.getEmpId())) {
                throw new DuplicateResourceException("Employee " + employee.getEmpId() + " already has an account");
            }
        } else if (request.role() != Role.ADMIN) {
            // Rule: only an ADMIN may exist without an employee record
            throw new BusinessRuleException("employeeId is required for MANAGER and EMPLOYEE accounts");
        }

        UserAccount account = new UserAccount();
        account.setEmployee(employee);
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setRole(request.role());
        account.setAccountStatus(AccountStatus.ACTIVE);

        return UserAccountResponse.from(userAccountRepository.save(account));
    }

    public List<UserAccountResponse> getAllAccounts() {
        return userAccountRepository.findAll().stream().map(UserAccountResponse::from).toList();
    }

    public UserAccountResponse getAccountById(Long accountId) {
        return UserAccountResponse.from(findAccount(accountId));
    }

    /** Accounts are never deleted - they are deactivated. */
    @Transactional
    public UserAccountResponse deactivate(Long accountId) {
        return changeStatus(accountId, AccountStatus.INACTIVE);
    }

    @Transactional
    public UserAccountResponse activate(Long accountId) {
        return changeStatus(accountId, AccountStatus.ACTIVE);
    }

    private UserAccountResponse changeStatus(Long accountId, AccountStatus status) {
        UserAccount account = findAccount(accountId);
        account.setAccountStatus(status);
        return UserAccountResponse.from(userAccountRepository.save(account));
    }

    private UserAccount findAccount(Long accountId) {
        return userAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id " + accountId));
    }
}
