package org.example.transfer.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.transfer.comm.BaseConstant;
import org.example.transfer.comm.enums.ExceptionEnum;
import org.example.transfer.comm.enums.TransferTypeEnum;
import org.example.transfer.config.TransferConfig;
import org.example.transfer.exception.BusinessException;
import org.example.transfer.model.Account;
import org.example.transfer.model.FxRate;
import org.example.transfer.model.TransferLog;
import org.example.transfer.repository.AccountRepository;
import org.example.transfer.repository.FxRateRepository;
import org.example.transfer.repository.TransferLogRepository;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Component
public class DifferentCurrencyTransferHandler implements TransferHandler {
    Logger log = LogManager.getLogger(TransferHandlerFactory.class);

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private FxRateRepository fxRateRepository;

    @Autowired
    private TransferLogRepository transferLogRepository;

    @Autowired
    private TransferConfig transferConfig;

    @Override
    public TransferTypeEnum getTransferType() {
        return TransferTypeEnum.DIFFERENT;
    }

    /**
     * transfer
     * <p>
     * diff currency:
     * from -> fromCurrency(USD) -> fxRate(USD->JPN) -> toCurrency(JPN) -> to
     *
     * @param from   source account
     * @param to     target account
     * @param amount req amount
     */
    @Override
    public void transfer(Account from, Account to, BigDecimal amount) {
        String traceId = MDC.get("traceId");

        if (!transferConfig.isEnableDifferentCurrencyTransfer()) {
            log.error("traceId:{}, not support transfer type:{}, fromCurrency:{}, toCurrency:{}",
                    traceId, getTransferType(), from.getCurrency(), to.getCurrency());
            throw new BusinessException(ExceptionEnum.TRANSFER_TYPE_NOT_SUPPORT.getErrorCode(),
                    String.format("not support transfer type: %s, fromCurrency:%s, toCurrency:%s",
                            getTransferType(), from.getCurrency(), to.getCurrency()));
        } else {
            /*
                1. check exchange rate
                    1.1 not exist, can't transfer
                    1.2 exist, transfer
            */
            // todo get fxRate from redis
            Optional<FxRate> fxRateOptional = fxRateRepository.findByFromCurrencyAndToCurrency(from.getCurrency(), to.getCurrency());
            if (fxRateOptional.isEmpty()) {
                log.error("traceId:{}, receiver:[{}] doesn't support:[{}], and no existing rate support, toCurrency:[{}]",
                        traceId, to.getId(), from.getCurrency(), to.getCurrency());
                throw new BusinessException(ExceptionEnum.RATE_NOT_SUPPORT.getErrorCode(), ExceptionEnum.RATE_NOT_SUPPORT.getErrorMsg());
            }

            BigDecimal fxRate = fxRateOptional.get().getRate();
            BigDecimal fee = amount.multiply(BigDecimal.valueOf(BaseConstant.FEE_RATE)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalDeduct = amount.add(fee).setScale(2, RoundingMode.HALF_UP);

            // 2. check balance
            if (from.getBalance().compareTo(totalDeduct) < 0) {
                log.error("traceId:{}, insufficient balance, from:[{}], fromBalance:[{}], totalDeduct:[{}]",
                        traceId, from.getId(), from.getBalance(), totalDeduct);
                throw new BusinessException(ExceptionEnum.MONEY_TRANSFER_ERROR.getErrorCode(), "Insufficient balance");
            }

            // 3. deduct from balance
            from.setBalance(from.getBalance().subtract(totalDeduct));

            // 4. rate exchange
            BigDecimal convertedAmount = amount.multiply(fxRate).setScale(2, RoundingMode.HALF_UP);

            // 5. add to balance
            to.setBalance(to.getBalance().add(convertedAmount));

            // 6. save to db
            accountRepository.save(from);
            accountRepository.save(to);

            // 7. save log
            TransferLog transferLog = new TransferLog();
            transferLog.setFromAccountId(from.getId());
            transferLog.setFromCurrency(from.getCurrency());
            transferLog.setToAccountId(to.getId());
            transferLog.setToCurrency(to.getCurrency());
            transferLog.setAmount(amount);
            transferLog.setFee(fee);
            transferLog.setFxRate(fxRate);
            transferLogRepository.save(transferLog);
        }




    }
}
