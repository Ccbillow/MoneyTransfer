package org.example.transfer.service.impl;

import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.transfer.comm.enums.ExceptionEnum;
import org.example.transfer.comm.enums.TransferTypeEnum;
import org.example.transfer.exception.BusinessException;
import org.example.transfer.executor.CircuitBreakerExecutor;
import org.example.transfer.executor.IdempotentExecutor;
import org.example.transfer.executor.OptimisticRetryExecutor;
import org.example.transfer.executor.RateLimiterExecutor;
import org.example.transfer.handler.TransferHandlerFactory;
import org.example.transfer.model.Account;
import org.example.transfer.params.req.TransferRequest;
import org.example.transfer.repository.AccountRepository;
import org.example.transfer.repository.FxRateRepository;
import org.example.transfer.repository.TransferLogRepository;
import org.example.transfer.service.TransferService;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private CircuitBreakerExecutor circuitBreakerExecutor;

    @Autowired
    private RateLimiterExecutor rateLimiterExecutor;

//    @Autowired
//    private RedisLockExecutor redisLockExecutor;

    @Autowired
    private IdempotentExecutor idempotentExecutor;

    @Autowired
    private TransferHandlerFactory transferHandlerFactory;

    @Override
    public void transfer(TransferRequest request) {
//        String lockKey = String.format("transfer-lock:%d-%d",
//                Math.min(request.getFromId(), request.getToId()),
//                Math.max(request.getFromId(), request.getToId()));
//
//        idempotentExecutor.executeWithIdempotency(request.getRequestId(), 3600, () ->
//                rateLimiterExecutor.execute(() ->
//                        circuitBreakerExecutor.execute(() ->
//                                redisLockExecutor.executeWithLock(lockKey, 5, 10, () ->
//                                        retryExecutor.executeWithRetry(() ->
//                                                doTransfer(request))))));

        idempotentExecutor.execute(request.getRequestId(), () ->
                rateLimiterExecutor.execute(() ->
                        circuitBreakerExecutor.execute(() ->
                                retryExecutor.executeWithRetry(() ->
                                        doTransfer(request)))));

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

        // 3. transfer
        TransferTypeEnum type = to.getCurrency().equals(request.getTransferCurrency())
                ? TransferTypeEnum.SAME
                : TransferTypeEnum.DIFFERENT;
        transferHandlerFactory.getHandler(type).transfer(from, to, request.getAmount());
    }

    /**
     * check user
     *
     * @param fromId  sender
     * @param toId    receiver
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