package org.example.controller;

import jakarta.transaction.Transactional;
import org.example.BaseTest;
import org.example.comm.enums.Currency;
import org.example.comm.enums.ExceptionEnum;
import org.example.model.Account;
import org.example.model.FxRate;
import org.example.params.req.TransferRequest;
import org.example.params.resp.CommonResponse;
import org.example.repository.AccountRepository;
import org.example.repository.FxRateRepository;
import org.example.util.JsonUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * transfer controller test
 */
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TransferControllerTest extends BaseTest {

    private final static String DEFAULT_ACCOUNR_PARH = "testdata/accounts_default.json";
    private final static String DEFAULT_RATE_PARH = "testdata/rate_default.json";
    private final static String TRANSFER_URL = "/api/transfer";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private FxRateRepository fxRateRepository;

    @MockBean
    private CommandLineRunner myDataInitializer;

    @Test
    public void testTransfer_USD_From_Alice_To_Bob_Fail() throws Exception {
        setup(DEFAULT_ACCOUNR_PARH, DEFAULT_RATE_PARH);

        TransferRequest request = new TransferRequest();
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
    public void testRepeatTransfer_AUD_From_Bob_To_Alice_20_Times_Fail() throws Exception {
        setup(DEFAULT_ACCOUNR_PARH, DEFAULT_RATE_PARH);

        List<CommonResponse<Void>> responses = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            TransferRequest request = new TransferRequest();
            request.setFromId(2L);
            request.setToId(1L);
            request.setTransferCurrency(Currency.USD);
            request.setAmount(BigDecimal.valueOf(50));

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
    public void testConcurrentTransfer_Fail() throws Exception {
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
    public void testTransfer_USD_From_Alice_To_Bob_Success() throws Exception {
        setup("testdata/accounts_test_one.json", "testdata/rate_test_one.json");

        TransferRequest request = new TransferRequest();
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

    @Test
    public void testConcurrentTransfer_OptimisticLockException() throws Exception {
        setup("testdata/accounts_test_one.json", "testdata/rate_test_one.json");
        ExecutorService executor = Executors.newFixedThreadPool(3);

        List<Callable<CommonResponse<Void>>> tasks = List.of(
                // Transfer 20 AUD from Bob to Alice
                () -> send(1L, 2L, 20, Currency.USD),
                // Transfer money from 40 USD  Alice to bob
                () -> send(1L, 2L, 40, Currency.USD),
                // Transfer money from 40 CNY  Alice to bob
                () -> send(1L, 2L, 60, Currency.USD),
                () -> send(1L, 2L, 10, Currency.USD),
                () -> send(1L, 2L, 20, Currency.USD),
                () -> send(1L, 2L, 1, Currency.USD),
                () -> send(1L, 2L, 1, Currency.USD),
                () -> send(1L, 2L, 1, Currency.USD),
                () -> send(1L, 2L, 1, Currency.USD),
                () -> send(1L, 2L, 1, Currency.USD)
        );

        List<Future<CommonResponse<Void>>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        int successCount = 0;
        int optimisticLockFailureCount = 0;
        for (Future<CommonResponse<Void>> future : futures) {
            CommonResponse<Void> response = future.get();
            if (response.isSuccess()) {
                successCount++;
            } else if (ExceptionEnum.OPTIMISTIC_LOCK_ERROR.getErrorCode().equals(response.getErrorCode())) {
                optimisticLockFailureCount++;
            } else {
                Assert.fail("Unexpected response code: " + response.getErrorMsg());
            }
        }

        assertEquals(10,successCount + optimisticLockFailureCount);
        assertTrue(optimisticLockFailureCount > 0, "Expected some optimistic lock failures");
        assertTrue(successCount > 0, "Expected some successful transfers");
    }

    @Transactional
    void setup(String accountPath, String ratePath) {
        // del old data
        accountRepository.deleteAll();
        fxRateRepository.deleteAll();

        // load new data
        List<Account> accounts = JsonUtils.fromPathToObjList(accountPath, Account.class);
        List<FxRate> balances = JsonUtils.fromPathToObjList(ratePath, FxRate.class);

        accountRepository.saveAll(accounts);
        fxRateRepository.saveAll(balances);
    }

    private CommonResponse<Void> send(Long fromId, Long toId, double amount, Currency currency) throws Exception {
        try {
            TransferRequest request = new TransferRequest();
            request.setFromId(fromId);
            request.setToId(toId);
            request.setAmount(BigDecimal.valueOf(amount));
            request.setTransferCurrency(currency);

            String content = mockMvc.perform(post(TRANSFER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(Objects.requireNonNull(JsonUtils.toJson(request))))
                    .andReturn().getResponse().getContentAsString();
            return JsonUtils.fromJson(content, CommonResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new CommonResponse<>();
        }
    }

    private void verifyBalance(Long accountId, BigDecimal expectedBalances) {
        accountRepository.findById(accountId).ifPresent(account ->
                assertEquals(0, account.getBalance().compareTo(expectedBalances)));
    }
}
