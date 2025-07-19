package org.example.service;

import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.comm.BaseConstant;
import org.example.comm.enums.Currency;
import org.example.comm.enums.ExceptionEnum;
import org.example.exception.BusinessException;
import org.example.model.Account;
import org.example.model.FxRate;
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
    Logger log = LogManager.getLogger(TransferServiceImpl.class);

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private FxRateRepository fxRateRepository;

    @Override
    @Transactional
    public void transfer(TransferRequest request) {
        Account from = accountRepository.findById(request.getFromId()).orElseThrow(() -> {
            log.error("money transfer fail, sender account not exist, from:[{}]", request.getFromId());
            return new BusinessException(ExceptionEnum.USER_NOT_EXIST.getErrorCode(), "from account not exist");
        });
        Account to = accountRepository.findById(request.getToId()).orElseThrow(() -> {
            log.error("money transfer fail, receiver account not exist, from:[{}]", request.getToId());
            return new BusinessException(ExceptionEnum.USER_NOT_EXIST.getErrorCode(), "to account not exist");
        });

        Currency requestCurrency = request.getTransferCurrency();
        BigDecimal requestAmount = request.getAmount();

        // 1. check from currency
        if (from.getCurrency() != requestCurrency) {
            log.error("money transfer fail, sender must use base currency, sender:[{}], baseCurrency:[{}], requestCurrency:[{}]",
                    request.getFromId(), from.getCurrency(), requestCurrency);
            throw new BusinessException(ExceptionEnum.PARAM_ILLEGAL.getErrorCode(), "Sender must use base currency.");
        }

        // 2. to same currency
        if (to.getCurrency() == requestCurrency) {
            performTransfer(from, to, requestAmount, BigDecimal.ONE);
            return;
        }

        // 3. check exchange rate
        Optional<FxRate> fxRate = fxRateRepository.findByFromCurrencyAndToCurrency(from.getCurrency(), to.getCurrency());
        if (fxRate.isEmpty()) {
            log.error("money transfer fail, receiver:[{}] doesn't support:[{}], and no existing rate support, toCurrency:[{}]",
                    to.getId(), requestCurrency, to.getCurrency());
            throw new BusinessException(ExceptionEnum.RATE_NOT_SUPPORT.getErrorCode(), ExceptionEnum.RATE_NOT_SUPPORT.getErrorMsg());
        }

        performTransfer(from, to, requestAmount, fxRate.get().getRate());
    }

    /**
     * transfer
     *
     * @param from          source account
     * @param to            target account
     * @param requestAmount req amount
     * @param fxRate        money exchange rate
     */
    private void performTransfer(Account from, Account to, BigDecimal requestAmount, BigDecimal fxRate) {
        BigDecimal fee = requestAmount.multiply(BigDecimal.valueOf(BaseConstant.FEE_RATE));
        BigDecimal totalDeduct = requestAmount.add(fee);

        // 1. check balance
        if (from.getBalance().compareTo(totalDeduct) < 0) {
            log.error("money transfer fail, insufficient balance, from:[{}], fromBalance:[{}], totalDeduct:[{}]",
                    from.getId(), from.getBalance(), totalDeduct);
            throw new BusinessException(ExceptionEnum.MONEY_TRANSFER_ERROR.getErrorCode(), "Insufficient balance");
        }

        // 2. deduct from balance
        from.setBalance(from.getBalance().subtract(totalDeduct));

        // 3. rate exchange
        BigDecimal convertedAmount = requestAmount.multiply(fxRate);

        // 4. add to balance
        to.setBalance(to.getBalance().add(convertedAmount));

        // 5. save to db
        accountRepository.save(from);
        accountRepository.save(to);
    }
}