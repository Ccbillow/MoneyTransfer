package org.example.repository;

import org.example.comm.enums.Currency;
import org.example.model.FxRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FxRateRepository extends JpaRepository<FxRate, Long> {

    Optional<FxRate> findByFromCurrencyAndToCurrency(Currency from, Currency to);
}