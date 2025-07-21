package org.example.transfer.params.req;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.example.transfer.comm.enums.Currency;

import java.math.BigDecimal;

public class TransferRequest {

    /**
     * requestId
     * for idempotent check
     */
//    @NotNull(message = "requestId can not be null")
//    private String requestId;

    @NotNull(message = "fromId can not be null")
    private Long fromId;

    @NotNull(message = "toId can not be null")
    private Long toId;

    @NotNull(message = "amount can not be null")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    /**
     * transfer currency
     * default using from account basic currency
     */
    @NotNull(message = "transferCurrency can not be null")
    private Currency transferCurrency;

    public Long getFromId() {
        return fromId;
    }

    public void setFromId(Long fromId) {
        this.fromId = fromId;
    }

    public Long getToId() {
        return toId;
    }

    public void setToId(Long toId) {
        this.toId = toId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Currency getTransferCurrency() {
        return transferCurrency;
    }

    public void setTransferCurrency(Currency transferCurrency) {
        this.transferCurrency = transferCurrency;
    }
}
