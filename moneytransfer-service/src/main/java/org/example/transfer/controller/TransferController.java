package org.example.transfer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.common.response.CommonResponse;
import org.example.transfer.params.req.TransferRequest;
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
@Tag(name = "Money Transfer Service", description = "transfer money")
@RestController
@RequestMapping("/api")
public class TransferController {

    @Autowired
    private TransferService transferService;

    @Operation(summary = "transfer moeny", description = "transfer money")
    @RequestMapping(value = "/transfer", method = RequestMethod.POST)
    @ResponseBody
    public CommonResponse<Void> transfer(@Parameter(description = "transfer money request")
                                             @RequestBody @Valid TransferRequest request) {
        CommonResponse<Void> result = new CommonResponse<>();
        transferService.transfer(request);
        result.setSuccess(true);
        return result;
    }
}