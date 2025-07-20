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
import org.slf4j.MDC;
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
        String traceId = MDC.get("traceId");
        Account from = accountRepository.findById(request.getFromId()).orElseThrow(() -> {
            log.error("traceId:{}, sender account not exist, from:[{}]", traceId, request.getFromId());
            return new BusinessException(ExceptionEnum.USER_NOT_EXIST.getErrorCode(), "from account not exist");
        });
        Account to = accountRepository.findById(request.getToId()).orElseThrow(() -> {
            log.error("traceId:{}, receiver account not exist, from:[{}]", traceId, request.getToId());
            return new BusinessException(ExceptionEnum.USER_NOT_EXIST.getErrorCode(), "to account not exist");
        });

        Currency requestCurrency = request.getTransferCurrency();
        BigDecimal requestAmount = request.getAmount();

        // 1. check from currency
        if (from.getCurrency() != requestCurrency) {
            log.error("traceId:{}, sender must use base currency, sender:[{}], baseCurrency:[{}], requestCurrency:[{}]",
                    traceId, request.getFromId(), from.getCurrency(), requestCurrency);
            throw new BusinessException(ExceptionEnum.PARAM_ILLEGAL.getErrorCode(), "Sender must use base currency.");
        }

        // 2. to same currency
        if (to.getCurrency() == requestCurrency) {
            performTransfer(from, to, requestAmount, BigDecimal.ONE, traceId);
            return;
        }

        // 3. check exchange rate
        Optional<FxRate> fxRate = fxRateRepository.findByFromCurrencyAndToCurrency(from.getCurrency(), to.getCurrency());
        if (fxRate.isEmpty()) {
            log.error("traceId:{}, receiver:[{}] doesn't support:[{}], and no existing rate support, toCurrency:[{}]",
                    traceId, to.getId(), requestCurrency, to.getCurrency());
            throw new BusinessException(ExceptionEnum.RATE_NOT_SUPPORT.getErrorCode(), ExceptionEnum.RATE_NOT_SUPPORT.getErrorMsg());
        }

        performTransfer(from, to, requestAmount, fxRate.get().getRate(), traceId);
    }

    /**
     * transfer
     *
     * @param from          source account
     * @param to            target account
     * @param requestAmount req amount
     * @param fxRate        money exchange rate
     */
    private void performTransfer(Account from, Account to, BigDecimal requestAmount, BigDecimal fxRate, String traceId) {
        BigDecimal fee = requestAmount.multiply(BigDecimal.valueOf(BaseConstant.FEE_RATE));
        BigDecimal totalDeduct = requestAmount.add(fee);

        // 1. check balance
        if (from.getBalance().compareTo(totalDeduct) < 0) {
            log.error("traceId:{}, insufficient balance, from:[{}], fromBalance:[{}], totalDeduct:[{}]",
                    traceId, from.getId(), from.getBalance(), totalDeduct);
            throw new BusinessException(ExceptionEnum.MONEY_TRANSFER_ERROR.getErrorCode(), "Insufficient balance");
        }

        // 2. deduct from balance
        from.setBalance(from.getBalance().subtract(totalDeduct));

        // 3. rate exchange
        BigDecimal convertedAmount = requestAmount.multiply(fxRate);

        // 4. add to balance
        to.setBalance(to.getBalance().add(convertedAmount));

        // 5. save to db
        // todo distribute lock in microservices (redis:redlock)
        // todo async, send to message queue
        accountRepository.save(from);
        accountRepository.save(to);
    }
}