package org.example.transfer.exception;

public class BusinessException extends RuntimeException{
    private static final long serialVersionUID = 1L;

    /**
     * error code
     */
    protected String errorCode;
    /**
     * error msg
     */
    protected String errorMsg;

    public BusinessException() {
        super();
    }

    public BusinessException(BaseErrorInfoInterface errorInfoInterface) {
        super(errorInfoInterface.getErrorCode());
        this.errorCode = errorInfoInterface.getErrorCode();
        this.errorMsg = errorInfoInterface.getErrorMsg();
    }

    public BusinessException(BaseErrorInfoInterface errorInfoInterface, Throwable cause) {
        super(errorInfoInterface.getErrorCode(), cause);
        this.errorCode = errorInfoInterface.getErrorCode();
        this.errorMsg = errorInfoInterface.getErrorMsg();
    }

    public BusinessException(String errorMsg) {
        super(errorMsg);
        this.errorMsg = errorMsg;
    }

    public BusinessException(String errorCode, String errorMsg) {
        super(errorCode);
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    public BusinessException(String errorCode, String errorMsg, Throwable cause) {
        super(errorCode, cause);
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
