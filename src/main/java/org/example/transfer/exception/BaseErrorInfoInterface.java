package org.example.transfer.exception;

public interface BaseErrorInfoInterface {

    /**
     *  error code
     * @return
     */
    String getErrorCode();

    /**
     * error msg
     * @return
     */
    String getErrorMsg();
}