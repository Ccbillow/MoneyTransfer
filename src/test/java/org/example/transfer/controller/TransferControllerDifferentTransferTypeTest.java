package org.example.transfer.controller;

import org.example.transfer.comm.enums.Currency;
import org.example.transfer.comm.enums.ExceptionEnum;
import org.example.transfer.params.req.TransferRequest;
import org.example.transfer.params.resp.CommonResponse;
import org.example.transfer.util.JsonUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * transfer controller test
 * <p>
 * only test enable different transfer type
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = "transfer.enable-different-currency-transfer=true")
public class TransferControllerDifferentTransferTypeTest extends BaseControllerTest {

    @Test
    public void testDiffTypeTransferUSDFromAliceToBob_Fail() throws Exception {
        setup(DEFAULT_ACCOUNR_PARH, DEFAULT_RATE_PARH);

        TransferRequest request = new TransferRequest();
        request.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        request.setFromId(1L);
        request.setToId(2L);
        request.setTransferCurrency(Currency.USD);
        request.setAmount(BigDecimal.valueOf(50));

        String content = mockMvc.perform(post(TRANSFER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(JsonUtils.toJson(request))))
                .andReturn().getResponse().getContentAsString();
        CommonResponse<Void> response = JsonUtils.fromJson(content, CommonResponse.class);

        assertNotNull(response);
        assertEquals(ExceptionEnum.RATE_NOT_SUPPORT.getErrorCode(), response.getErrorCode());
        assertEquals("not support rate!", response.getErrorMsg());
    }

    @Test
    public void testDiffTypeTransferAUDFromBobToAlice20Times_Fail() throws Exception {
        setup(DEFAULT_ACCOUNR_PARH, DEFAULT_RATE_PARH);

        List<CommonResponse<Void>> responses = new ArrayList<>();
        TransferRequest request = new TransferRequest();
        request.setFromId(2L);
        request.setToId(1L);
        request.setTransferCurrency(Currency.USD);
        request.setAmount(BigDecimal.valueOf(50));

        for (int i = 0; i < 20; i++) {
            request.setRequestId(UUID.randomUUID().toString().replace("-", ""));
            String content = mockMvc.perform(post(TRANSFER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(Objects.requireNonNull(JsonUtils.toJson(request))))
                    .andReturn().getResponse().getContentAsString();
            CommonResponse<Void> response = JsonUtils.fromJson(content, CommonResponse.class);
            responses.add(response);

            assertNotNull(response);
            assertEquals(ExceptionEnum.PARAM_ILLEGAL.getErrorCode(), response.getErrorCode());
            assertEquals("Sender must use base currency.", response.getErrorMsg());
        }

        assertEquals(20, responses.size());
    }

    @Test
    public void testDiffTypeConcurrentTransfer_Fail() throws Exception {
        setup(DEFAULT_ACCOUNR_PARH, DEFAULT_RATE_PARH);
        ExecutorService executor = Executors.newFixedThreadPool(3);

        List<Callable<CommonResponse<Void>>> tasks = List.of(
                // Transfer 20 AUD from Bob to Alice
                () -> send(2L, 1L, 20, Currency.AUD),
                // Transfer money from 40 USD  Alice to bob
                () -> send(1L, 2L, 40, Currency.USD),
                // Transfer money from 40 CNY  Alice to bob
                () -> send(1L, 2L, 40, Currency.CNY)
        );

        List<Future<CommonResponse<Void>>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // verify response
        assertEquals(3, futures.size());

        CommonResponse<Void> r1 = futures.get(0).get(); // AUD from Bob to Alice
        CommonResponse<Void> r2 = futures.get(1).get(); // USD from Alice to Bob
        CommonResponse<Void> r3 = futures.get(2).get(); // CNY from Alice to Bob

        // verify r1, sender base currency
        assertNotNull(r1);
        assertEquals(ExceptionEnum.PARAM_ILLEGAL.getErrorCode(), r1.getErrorCode());
        assertEquals("Sender must use base currency.", r1.getErrorMsg());

        // verify r2, not supporting rate
        assertNotNull(r2);
        assertEquals(ExceptionEnum.RATE_NOT_SUPPORT.getErrorCode(), r2.getErrorCode());
        assertEquals("not support rate!", r2.getErrorMsg());

        // verify r3, sender base currency
        assertNotNull(r3);
        assertEquals(ExceptionEnum.PARAM_ILLEGAL.getErrorCode(), r3.getErrorCode());
        assertEquals("Sender must use base currency.", r3.getErrorMsg());
    }

    /**
     * if support rate
     * usd -> jpn
     *
     * @throws Exception
     */
    @Test
    public void testTransferUSDFromAliceToBob_Success() throws Exception {
        setup("testdata/accounts_test_one.json", "testdata/rate_test_one.json");

        TransferRequest request = new TransferRequest();
        request.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        request.setFromId(1L);
        request.setToId(2L);
        request.setTransferCurrency(Currency.USD);
        request.setAmount(BigDecimal.valueOf(50));

        String content = mockMvc.perform(post(TRANSFER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(JsonUtils.toJson(request))))
                .andReturn().getResponse().getContentAsString();
        CommonResponse<Void> response = JsonUtils.fromJson(content, CommonResponse.class);

        assertNotNull(response);
        assertNotNull(transferLogRepository.findAll());
        assertEquals(1, transferLogRepository.findAll().size());

        // verify balance
        verifyBalance(1L, BigDecimal.valueOf(949.50));
        verifyBalance(2L, BigDecimal.valueOf(8000.00));
    }

    /**
     * if support rate
     * usd -> jpn
     *
     * @throws Exception
     */
    @Test
    public void testConcurrentTransfer_Success() throws Exception {
        setup("testdata/accounts_test_three.json", "testdata/rate_test_three.json");
        ExecutorService executor = Executors.newFixedThreadPool(3);

        List<Callable<CommonResponse<Void>>> tasks = List.of(
                // Transfer 20 AUD from Bob to Alice
                () -> send(2L, 1L, 20, Currency.AUD),
                // Transfer money from 40 USD  Alice to bob
                () -> send(1L, 2L, 40, Currency.USD),
                // Transfer money from 40 CNY  Alice to bob
                () -> send(1L, 2L, 40, Currency.CNY)
        );

        List<Future<CommonResponse<Void>>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // verify response
        assertEquals(3, futures.size());

        CommonResponse<Void> r1 = futures.get(0).get(); // AUD from Bob to Alice
        CommonResponse<Void> r2 = futures.get(1).get(); // USD from Alice to Bob
        CommonResponse<Void> r3 = futures.get(2).get(); // CNY from Alice to Bob

        // verify r1, sender base currency
        assertNotNull(r1);
        assertEquals(ExceptionEnum.PARAM_ILLEGAL.getErrorCode(), r1.getErrorCode());
        assertEquals("Sender must use base currency.", r1.getErrorMsg());

        // verify r2, if support rate
        // verify balance
        assertNotNull(r2);
        verifyBalance(1L, BigDecimal.valueOf(959.60));
        verifyBalance(2L, BigDecimal.valueOf(6500.00));

        // verify r3, sender base currency
        assertNotNull(r3);
        assertEquals(ExceptionEnum.PARAM_ILLEGAL.getErrorCode(), r3.getErrorCode());
        assertEquals("Sender must use base currency.", r3.getErrorMsg());
    }
}
