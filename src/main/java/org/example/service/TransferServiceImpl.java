package org.example.service;

import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.comm.BaseConstant;
import org.example.comm.enums.ExceptionEnum;
import org.example.exception.BusinessException;
import org.example.executor.OptimisticRetryExecutor;
import org.example.model.Account;
import org.example.model.FxRate;
import org.example.model.TransferLog;
import org.example.params.req.TransferRequest;
import org.example.repository.AccountRepository;
import org.example.repository.FxRateRepository;
import org.example.repository.TransferLogRepository;
import org.example.service.impl.TransferService;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TransferServiceImpl implements TransferService {
    Logger log = LogManager.getLogger(TransferServiceImpl.class);

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private FxRateRepository fxRateRepository;

    @Autowired
    private TransferLogRepository transferLogRepository;

    @Autowired
    private OptimisticRetryExecutor retryExecutor;

//    @Autowired
//    private RedisLockExecutor redisLockExecutor;

    @Override
    public void transfer(TransferRequest request) {
//        String lockKey = String.format("transfer-lock:%d-%d",
//                Math.min(request.getFromId(), request.getToId()),
//                Math.max(request.getFromId(), request.getToId()));
//        redisLockExecutor.executeWithLock(lockKey,
//                5,
//                10,
//                () -> retryExecutor.executeWithRetry(() -> doTransfer(request)));

        retryExecutor.executeWithRetry(() -> doTransfer(request));
    }

    @Transactional
    public void doTransfer(TransferRequest request) {
        String traceId = MDC.get("traceId");

        // 1. check user
        Map<Long, Account> accountMap = checkUser(request.getFromId(), request.getToId(), traceId);
        Account from = accountMap.get(request.getFromId());
        Account to = accountMap.get(request.getToId());

        // 2. check from currency
        if (!from.getCurrency().equals(request.getTransferCurrency())) {
            log.error("traceId:{}, sender must use base currency, sender:[{}], baseCurrency:[{}], requestCurrency:[{}]",
                    traceId, request.getFromId(), from.getCurrency(), request.getTransferCurrency());
            throw new BusinessException(ExceptionEnum.PARAM_ILLEGAL.getErrorCode(), "Sender must use base currency.");
        }

        // 3. to same currency
        if (to.getCurrency().equals(request.getTransferCurrency())) {
            performTransfer(from, to, request.getAmount(), BigDecimal.ONE, traceId);
            return;
        }

        /*
            4. check exchange rate
                3.1 not exist, can't transfer
                3.1 exist, transfer
         */
        // todo get fxRate from redis
        Optional<FxRate> fxRate = fxRateRepository.findByFromCurrencyAndToCurrency(from.getCurrency(), to.getCurrency());
        if (fxRate.isEmpty()) {
            log.error("traceId:{}, receiver:[{}] doesn't support:[{}], and no existing rate support, toCurrency:[{}]",
                    traceId, to.getId(), request.getTransferCurrency(), to.getCurrency());
            throw new BusinessException(ExceptionEnum.RATE_NOT_SUPPORT.getErrorCode(), ExceptionEnum.RATE_NOT_SUPPORT.getErrorMsg());
        }

        performTransfer(from, to, request.getAmount(), fxRate.get().getRate(), traceId);
    }

    /**
     * transfer
     *
     * same currency:
     *      from -> fromCurrency(USD) -> toCurrency(JPN) -> to
     *
     * diff currency:
     *      from -> fromCurrency(USD) -> fxRate(USD->JPN) -> toCurrency(JPN) -> to
     *
     * @param from          source account
     * @param to            target account
     * @param requestAmount req amount
     * @param fxRate        money exchange rate
     */
    private void performTransfer(Account from, Account to, BigDecimal requestAmount, BigDecimal fxRate, String traceId) {
        BigDecimal fee = requestAmount.multiply(BigDecimal.valueOf(BaseConstant.FEE_RATE)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDeduct = requestAmount.add(fee).setScale(2, RoundingMode.HALF_UP);

        // 1. check balance
        if (from.getBalance().compareTo(totalDeduct) < 0) {
            log.error("traceId:{}, insufficient balance, from:[{}], fromBalance:[{}], totalDeduct:[{}]",
                    traceId, from.getId(), from.getBalance(), totalDeduct);
            throw new BusinessException(ExceptionEnum.MONEY_TRANSFER_ERROR.getErrorCode(), "Insufficient balance");
        }

        // 2. deduct from balance
        from.setBalance(from.getBalance().subtract(totalDeduct));

        // 3. rate exchange
        BigDecimal convertedAmount = requestAmount.multiply(fxRate).setScale(2, RoundingMode.HALF_UP);

        // 4. add to balance
        to.setBalance(to.getBalance().add(convertedAmount));

        // 5. save to db
        // todo distribute lock in microservices (redis:redlock)
        // todo async, send to message queue
        accountRepository.save(from);
        accountRepository.save(to);

        // 6. save log
        TransferLog transferLog = new TransferLog();
        transferLog.setFromAccountId(from.getId());
        transferLog.setFromCurrency(from.getCurrency());
        transferLog.setToAccountId(to.getId());
        transferLog.setToCurrency(to.getCurrency());
        transferLog.setAmount(requestAmount);
        transferLog.setFee(fee);
        transferLog.setFxRate(fxRate);
        transferLogRepository.save(transferLog);
    }

    /**
     * check user
     *
     * @param fromId sender
     * @param toId receiver
     * @param traceId uuid
     * @return Map<userId, Account>
     */
    private Map<Long, Account> checkUser(Long fromId, Long toId, String traceId) {
        if (fromId.equals(toId)) {
            log.warn("traceId:{}, same account transfer not allowed, from:[{}], to:[{}]",
                    traceId, fromId, toId);
            throw new BusinessException(ExceptionEnum.PARAM_ILLEGAL.getErrorCode(), "same account transfer not allowed");
        }

        List<Account> accounts = accountRepository.findAllById(Arrays.asList(fromId, toId));
        Map<Long, Account> accountMap = accounts.stream().collect(Collectors.toMap(Account::getId, Function.identity()));
        if (accountMap.get(fromId) == null) {
            log.error("traceId:{}, sender account not exist, from:[{}]", traceId, fromId);
            throw new BusinessException(ExceptionEnum.USER_NOT_EXIST.getErrorCode(), "from account not exist");
        }
        if (accountMap.get(toId) == null) {
            log.error("traceId:{}, receiver account not exist, to:[{}]", traceId, fromId);
            throw new BusinessException(ExceptionEnum.USER_NOT_EXIST.getErrorCode(), "to account not exist");
        }
        return accountMap;
    }
}