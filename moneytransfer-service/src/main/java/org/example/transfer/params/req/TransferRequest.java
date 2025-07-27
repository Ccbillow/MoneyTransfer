package org.example.transfer.params.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.example.transfer.comm.enums.Currency;

import java.math.BigDecimal;

@Schema(description = "transfer request")
public class TransferRequest {

    /**
     * requestId
     * for idempotent check
     */
    @Schema(description = "requestId", example = "AAA111")
    @NotNull(message = "requestId can not be null")
    private String requestId;

    @Schema(description = "from user id", example = "1")
    @NotNull(message = "fromId can not be null")
    private Long fromId;

    @Schema(description = "to user id", example = "2")
    @NotNull(message = "toId can not be null")
    private Long toId;

    @Schema(description = "amount", example = "1.00")
    @NotNull(message = "amount can not be null")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    /**
     * transfer currency
     * default using from account basic currency
     */
    @Schema(description = "transfer currency", example = "USD")
    @NotNull(message = "transferCurrency can not be null")
    private Currency transferCurrency;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

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
