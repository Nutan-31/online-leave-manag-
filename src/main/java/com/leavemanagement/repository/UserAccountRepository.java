package com.leavemanagement.repository;

import com.leavemanagement.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmployeeEmpId(Long empId);

    Optional<UserAccount> findByEmployeeEmpId(Long empId);

    Optional<UserAccount> findByUsername(String username);
}
