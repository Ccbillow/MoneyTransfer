package org.example.service;

import jakarta.transaction.Transactional;
import org.example.comm.BaseConstant;
import org.example.model.Account;
import org.example.model.FxRate;
import org.example.comm.enums.Currency;
import org.example.params.req.TransferRequest;
import org.example.repository.AccountRepository;
import org.example.repository.FxRateRepository;
import org.example.service.impl.TransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class TransferServiceImpl implements TransferService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private FxRateRepository fxRateRepository;

    @Override
    @Transactional
    public void transfer(TransferRequest request) {
        Account from = accountRepository.findById(request.getFromId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid from account"));
        Account to = accountRepository.findById(request.getToId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid to account"));

        Currency requestCurrency = request.getTransferCurrency();
        BigDecimal requestAmount = request.getAmount();

        // 1. check from currency
        if (from.getCurrency() != request.getTransferCurrency()) {
            throw new IllegalArgumentException("Sender must transfer using their base currency");
        }

        // 2. to same currency
        if (to.getCurrency() == request.getTransferCurrency()) {
            performTransfer(from, to, requestAmount, requestCurrency, requestCurrency, BigDecimal.ONE);
            return;
        }

        // 3. check exchange rate
        Optional<FxRate> fxRate = fxRateRepository.findByFromCurrencyAndToCurrency(from.getCurrency(), to.getCurrency());
        if (fxRate.isEmpty()) {
            throw new IllegalArgumentException("Receiver not support: " + requestCurrency + "，no existing rate to exchange");
        }

        performTransfer(from, to, requestAmount, requestCurrency, fxRate.get().getToCurrency(), fxRate.get().getRate());
    }

    /**
     * transfer
     * @param from source account
     * @param to target account
     * @param requestAmount req amount
     * @param fromCurrency source currency
     * @param toCurrency target currency
     * @param fxRate money exchange rate
     */
    private void performTransfer(Account from, Account to, BigDecimal requestAmount,
                                 Currency fromCurrency, Currency toCurrency, BigDecimal fxRate) {
        BigDecimal fee = requestAmount.multiply(new BigDecimal(BaseConstant.FEE_RATE));
        BigDecimal totalDeduct = requestAmount.add(fee);

        // 1. check balance
        if (from.getBalance().compareTo(totalDeduct) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        // 2. deduct from balance
        from.setBalance(from.getBalance().subtract(totalDeduct));

        // 3. rate exchange
        BigDecimal convertedAmount = requestAmount.multiply(fxRate) ;

        // 4. add to balance
        to.setBalance(to.getBalance().add(convertedAmount));

        // 5. save to db
        accountRepository.save(from);
        accountRepository.save(to);
    }
}