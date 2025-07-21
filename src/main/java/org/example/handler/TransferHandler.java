package org.example.handler;

import org.example.comm.enums.TransferTypeEnum;
import org.example.model.Account;

import java.math.BigDecimal;

public interface TransferHandler {

    /**
     * get transfer type
     * @return transfer type
     */
    TransferTypeEnum getTransferType();

    /**
     * support different types of money transfer
     *
     * @param from      transfer sender
     * @param to        transfer receiver
     * @param amount    transfer amount
     */
    void transfer(Account from, Account to, BigDecimal amount);
}
