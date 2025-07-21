package org.example.transfer.repository;

import org.example.transfer.comm.enums.Currency;
import org.example.transfer.model.FxRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FxRateRepository extends JpaRepository<FxRate, Long> {

    Optional<FxRate> findByFromCurrencyAndToCurrency(Currency from, Currency to);
}