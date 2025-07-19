package org.example.service.impl;

import org.example.params.req.TransferRequest;

import java.math.BigDecimal;

public interface TransferService {

    public void transfer(TransferRequest request);
}