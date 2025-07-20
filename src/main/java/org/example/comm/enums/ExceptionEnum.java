package org.example.comm.enums;

import org.example.exception.BaseErrorInfoInterface;

/**
 * exception enum
 */
public enum ExceptionEnum implements BaseErrorInfoInterface {

    PARAM_ILLEGAL("4001","param illegal!"),
    NOT_FOUND("4004", "not found resources!"),
    USER_NOT_EXIST("4008", "user not exist!"),
    RATE_NOT_SUPPORT("4009", "not support rate!"),
    MONEY_TRANSFER_ERROR("4010", "money transfer error!"),
    OPTIMISTIC_LOCK_MAX_RETRY_ERROR("4011", "max retry exceeded due to optimistic locking!"),
    IDEMPOTENT_REQUEST("4012", "duplicate request!"),
    CIRCUIT_OPEN("4013", "service temporarily unavailable due to circuit breaker."),
    RATE_LIMIT_EXCEEDED("4014", "Too many requests, please try again later."),
    INTERNAL_SERVER_ERROR("5000", "internal server error!"),
    SERVER_BUSY("5003","server busy, please try later!");

    /**
     * error code
     */
    private final String errorCode;

    /**
     * error msg
     */
    private final String errorMsg;

    ExceptionEnum(String errorCode, String errorMsg) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String getErrorMsg() {
        return errorMsg;
    }
}
