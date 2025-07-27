package org.example.transfer.controller;

import jakarta.validation.Valid;
import org.example.transfer.params.req.TransferRequest;
import org.example.transfer.params.resp.CommonResponse;
import org.example.transfer.service.TransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * transfer controller
 */
@RestController
@RequestMapping("/api")
public class TransferController {

    @Autowired
    private TransferService transferService;

    @RequestMapping(value = "/transfer", method = RequestMethod.POST)
    @ResponseBody
    public CommonResponse<Void> transfer(@RequestBody @Valid TransferRequest request) {
        CommonResponse<Void> result = new CommonResponse<>();
        transferService.transfer(request);
        result.setSuccess(true);
        return result;
    }
}