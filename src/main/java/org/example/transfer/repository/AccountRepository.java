package org.example.transfer.repository;

import jakarta.transaction.Transactional;
import org.example.transfer.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<Account, Long> {
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM account", nativeQuery = true)
    void deleteAllAccountsNative();
}