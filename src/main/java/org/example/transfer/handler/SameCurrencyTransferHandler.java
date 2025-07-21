package org.example.transfer.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.transfer.comm.BaseConstant;
import org.example.transfer.comm.enums.ExceptionEnum;
import org.example.transfer.comm.enums.TransferTypeEnum;
import org.example.transfer.exception.BusinessException;
import org.example.transfer.model.Account;
import org.example.transfer.model.TransferLog;
import org.example.transfer.repository.AccountRepository;
import org.example.transfer.repository.TransferLogRepository;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class SameCurrencyTransferHandler implements TransferHandler {
    Logger log = LogManager.getLogger(TransferHandlerFactory.class);

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TransferLogRepository transferLogRepository;

    @Override
    public TransferTypeEnum getTransferType() {
        return TransferTypeEnum.SAME;
    }

    /**
     * transfer
     * <p>
     * same currency:
     * from -> fromCurrency(USD) -> toCurrency(JPN) -> to
     *
     * @param from   source account
     * @param to     target account
     * @param amount req amount
     */
    @Override
    public void transfer(Account from, Account to, BigDecimal amount) {
        String traceId = MDC.get("traceId");

        BigDecimal fee = amount.multiply(BigDecimal.valueOf(BaseConstant.FEE_RATE)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDeduct = amount.add(fee).setScale(2, RoundingMode.HALF_UP);

        // 1. check balance
        if (from.getBalance().compareTo(totalDeduct) < 0) {
            log.error("traceId:{}, insufficient balance, from:[{}], fromBalance:[{}], totalDeduct:[{}]",
                    traceId, from.getId(), from.getBalance(), totalDeduct);
            throw new BusinessException(ExceptionEnum.MONEY_TRANSFER_ERROR.getErrorCode(), "Insufficient balance");
        }

        // 2. deduct from balance
        from.setBalance(from.getBalance().subtract(totalDeduct));

        // 3. add to balance
        to.setBalance(to.getBalance().add(amount));

        // 4. save to db
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
        transferLog.setAmount(amount);
        transferLog.setFee(fee);
        transferLog.setFxRate(BigDecimal.ONE);
        transferLogRepository.save(transferLog);
    }
}
