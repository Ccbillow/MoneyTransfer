package org.example.config;

import org.example.model.Account;
import org.example.model.FxRate;
import org.example.comm.enums.Currency;
import org.example.repository.AccountRepository;
import org.example.repository.FxRateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * data init
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private FxRateRepository fxRateRepository;

    @Override
    public void run(String... args) {
        accountRepository.deleteAll();
        fxRateRepository.deleteAll();

        /**
         * Account 1
         * Name: Alice
         * Id:1
         * Initial amount :1000
         * Currency : USD
         */
        Account alice = new Account();
        alice.setName("Alice");
        alice.setCurrency(Currency.USD);
        alice.setBalance(BigDecimal.valueOf(1000));

        /**
         * Account 2
         * Name: Bob
         * Id:2
         * Initial amount :500
         * Currency : JPN
         */
        Account bob = new Account();
        bob.setName("Bob");
        bob.setCurrency(Currency.JPN);
        bob.setBalance(BigDecimal.valueOf(500));

        accountRepository.save(alice);
        accountRepository.save(bob);

        /**
         * The FX conversion rate is 0.50 USD to 1 AUD.
         */
        FxRate rate = new FxRate();
        rate.setId(1L);
        rate.setFromCurrency(Currency.USD);
        rate.setToCurrency(Currency.AUD);
        rate.setRate(BigDecimal.valueOf(2));
        fxRateRepository.save(rate);
    }
}