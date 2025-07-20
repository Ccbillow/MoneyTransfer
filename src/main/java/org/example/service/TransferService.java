package org.example.service;

import org.example.params.req.TransferRequest;

import java.math.BigDecimal;

public interface TransferService {

    /**
     * money transfer
     * @param request money transfer request
     */
    public void transfer(TransferRequest request);
}