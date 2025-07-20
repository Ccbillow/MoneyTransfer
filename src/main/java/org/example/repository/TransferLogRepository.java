package org.example.repository;

import org.example.model.TransferLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferLogRepository extends JpaRepository<TransferLog, Long> {
}