package org.example.service.impl;

import org.example.params.req.TransferRequest;

import java.math.BigDecimal;

public interface TransferService {

    /**
     * money transfer
     * @param request money transfer request
     */
    public void transfer(TransferRequest request);
}