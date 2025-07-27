package org.example.transfer.repository;

import org.example.transfer.model.TransferLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferLogRepository extends JpaRepository<TransferLog, Long> {
}