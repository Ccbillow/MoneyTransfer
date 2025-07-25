package org.example.transfer.controller;

import org.example.transfer.comm.enums.Currency;
import org.example.transfer.comm.enums.ExceptionEnum;
import org.example.transfer.params.req.TransferRequest;
import org.example.transfer.params.resp.CommonResponse;
import org.example.transfer.util.JsonUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * transfer controller performance test
 * <p>
 * all performance test base on same money transfer
 */
@AutoConfigureMockMvc
public class TransferControllerPerformenceTest extends BaseControllerTest {

    /**
     * Test the consistency of the amount after concurrent transfers
     * <p>
     * Condition
     * from money(usd): 100000 - 1 * 10 - 0.01 * 10 = 99989.9
     * to money(jpn): 500 + 10 = 510
     * tasks: 10
     * concurrent:3
     * <p>
     * Result: all tasks success
     */
    @Test
    public void testConcurrentTransferMoney_Success() throws Exception {
        setup("testdata/accounts_test_performance.json", "testdata/rate_test_performance.json");
        final Random random = new Random();
        int concurrent = 10;

        ExecutorService executor = Executors.newFixedThreadPool(3);
        List<Callable<CommonResponse<Void>>> tasks = IntStream.range(0, concurrent)
                .mapToObj(i -> (Callable<CommonResponse<Void>>) () -> send(1L, 2L, 1, Currency.USD))
                .collect(Collectors.toList());

        List<Future<CommonResponse<Void>>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        int successCount = 0;
        for (Future<CommonResponse<Void>> future : futures) {
            CommonResponse<Void> response = future.get();
            System.out.println("concurrent response:" + JsonUtils.toJson(response));
            if (response.isSuccess()) {
                successCount++;
            }
        }

        assertEquals(successCount, concurrent);
        verifyBalance(1L, BigDecimal.valueOf(99989.90));
        verifyBalance(2L, BigDecimal.valueOf(510));
    }


    /**
     * Test low-concurrency transfer, final success after retry
     * <p>
     * Condition
     * tasks: 10
     * concurrency:5
     * retry:3
     * <p>
     * Result: all success
     */
    @Test
    public void testLowConcurrentTransferRetry_Success() throws Exception {
        setup("testdata/accounts_test_performance.json", "testdata/rate_test_performance.json");
        final Random random = new Random();
        int concurrent = 10;

        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Callable<CommonResponse<Void>>> tasks = IntStream.range(0, concurrent)
                .mapToObj(i -> randomTransferTask(random))
                .collect(Collectors.toList());

        List<Future<CommonResponse<Void>>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        int successCount = 0;
        for (Future<CommonResponse<Void>> future : futures) {
            CommonResponse<Void> response = future.get();
            if (response.isSuccess()) {
                successCount++;
            }
        }

        assertEquals(successCount, concurrent);
    }

    /**
     * Test mid-concurrency transfer, final success after retry
     * <p>
     * Condition
     * tasks: 50
     * concurrency:50
     * retry:3
     * <p>
     * Result: all success
     */
    @Test
    public void testMidConcurrentTransferRetry_Success() throws Exception {
        setup("testdata/accounts_test_performance.json", "testdata/rate_test_performance.json");
        final Random random = new Random();
        int concurrent = 50;

        ExecutorService executor = Executors.newFixedThreadPool(concurrent);
        List<Callable<CommonResponse<Void>>> tasks = IntStream.range(0, concurrent)
                .mapToObj(i -> randomTransferTask(random))
                .collect(Collectors.toList());

        List<Future<CommonResponse<Void>>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        int successCount = 0;
        for (Future<CommonResponse<Void>> future : futures) {
            CommonResponse<Void> response = future.get();
            if (response.isSuccess()) {
                successCount++;
            }
        }

        assertEquals(successCount, concurrent);
    }

    /**
     * Test hign-concurrency transfer, final fail after retry
     * <p>
     * Condition
     * tasks: 1000
     * concurrency:1000
     * retry:3
     * <p>
     * Result: some tasks success, some tasks fail
     */
    @Test
    public void testHighConcurrentTransferRetry_Fail() throws Exception {
        setup("testdata/accounts_test_performance.json", "testdata/rate_test_performance.json");
        final Random random = new Random();
        int concurrent = 1000;

        ExecutorService executor = Executors.newFixedThreadPool(concurrent);
        List<Callable<CommonResponse<Void>>> tasks = IntStream.range(0, concurrent)
                .mapToObj(i -> randomTransferTask(random))
                .toList();

        List<Future<CommonResponse<Void>>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        int successCount = 0;
        int optimisticLockMaxRetryFailureCount = 0;
        for (Future<CommonResponse<Void>> future : futures) {
            CommonResponse<Void> response = future.get();
            if (response.isSuccess()) {
                successCount++;
            } else if (ExceptionEnum.OPTIMISTIC_LOCK_MAX_RETRY_ERROR.getErrorCode().equals(response.getErrorCode())) {
                optimisticLockMaxRetryFailureCount++;
            } else {
                fail("Unexpected response code: " + response.getErrorMsg());
            }
        }

        assertEquals(concurrent, successCount + optimisticLockMaxRetryFailureCount);
        assertTrue(successCount > 0, "Expected some successful transfers");
        assertTrue(optimisticLockMaxRetryFailureCount > 0, "Expected max retry fail transfers");
    }

    @Test
    public void testCircuitBreakerOpenState() throws Exception {
        setup(DEFAULT_ACCOUNR_PARH, DEFAULT_RATE_PARH);

        List<CommonResponse<Void>> responses = new ArrayList<>();
        TransferRequest request = new TransferRequest();
        request.setFromId(2L);
        request.setToId(1L);
        request.setTransferCurrency(Currency.USD);
        request.setAmount(BigDecimal.valueOf(50));

        for (int i = 0; i < 21; i++) {
            request.setRequestId(UUID.randomUUID().toString().replace("-", ""));
            String content = mockMvc.perform(post(TRANSFER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(Objects.requireNonNull(JsonUtils.toJson(request))))
                    .andReturn().getResponse().getContentAsString();
            CommonResponse<Void> response = JsonUtils.fromJson(content, CommonResponse.class);
            responses.add(response);
        }

        assertEquals(21, responses.size());

        assertTrue(responses.stream()
                .anyMatch(r -> ExceptionEnum.CIRCUIT_OPEN.getErrorCode().equals(r.getErrorCode())
                        && ExceptionEnum.CIRCUIT_OPEN.getErrorMsg().equals(r.getErrorMsg())));
    }

    @Test
    public void testRateLimiter() throws Exception {
        setup("testdata/accounts_test_performance.json", "testdata/rate_test_performance.json");
        final Random random = new Random();
        int concurrent = 1100;

        ExecutorService executor = Executors.newFixedThreadPool(concurrent);
        List<Callable<CommonResponse<Void>>> tasks = IntStream.range(0, concurrent)
                .mapToObj(i -> randomTransferTask(random))
                .toList();

        List<Future<CommonResponse<Void>>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        int successCount = 0;
        int optimisticLockMaxRetryFailureCount = 0;
        int rateLimitRejectCount = 0;
        for (Future<CommonResponse<Void>> future : futures) {
            CommonResponse<Void> response = future.get();
            if (response.isSuccess()) {
                successCount++;
            } else if (ExceptionEnum.OPTIMISTIC_LOCK_MAX_RETRY_ERROR.getErrorCode().equals(response.getErrorCode())) {
                optimisticLockMaxRetryFailureCount++;
            } else if (ExceptionEnum.RATE_LIMIT_EXCEEDED.getErrorCode().equals(response.getErrorCode())) {
                rateLimitRejectCount++;
            } else {
                fail("Unexpected response code: " + response.getErrorMsg());
            }
        }

        assertEquals(concurrent, successCount + rateLimitRejectCount + optimisticLockMaxRetryFailureCount);
        assertTrue(successCount > 0, "Expected some successful transfers");
        assertTrue(rateLimitRejectCount > 0, "Expected max retry fail transfers");
    }

    /**
     * Test idempotent behavior: same requestId should only be processed once
     * <p>
     * Condition:
     * - 10 concurrent requests
     * - Same requestId
     *
     * Expected:
     * - One success
     * - 9 duplicate error responses
     */
    @Test
    public void testIdempotent_DuplicateRequestRejected() throws Exception {
        setup("testdata/accounts_test_performance.json", "testdata/rate_test_performance.json");
        int concurrent = 10;

        String requestId = UUID.randomUUID().toString().replace("-", "");
        TransferRequest request = new TransferRequest();
        request.setFromId(1L);
        request.setToId(2L);
        request.setTransferCurrency(Currency.USD);
        request.setAmount(BigDecimal.valueOf(1));
        request.setRequestId(requestId);

        ExecutorService executor = Executors.newFixedThreadPool(concurrent);
        List<Callable<CommonResponse<Void>>> tasks = new ArrayList<>();
        for (int i = 0; i < concurrent; i++) {
            tasks.add(() -> {
                String content = mockMvc.perform(post(TRANSFER_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonUtils.toJson(request)))
                        .andReturn().getResponse().getContentAsString();
                return JsonUtils.fromJson(content, CommonResponse.class);
            });
        }

        List<Future<CommonResponse<Void>>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        int successCount = 0;
        int idempotentErrorCount = 0;

        for (Future<CommonResponse<Void>> future : futures) {
            CommonResponse<Void> response = future.get();
            if (response.isSuccess()) {
                successCount++;
            } else if (ExceptionEnum.IDEMPOTENT_REQUEST.getErrorCode().equals(response.getErrorCode())) {
                idempotentErrorCount++;
            } else {
                fail("Unexpected error: " + response.getErrorMsg());
            }
        }

        assertEquals(1, successCount);
        assertEquals(concurrent - 1, idempotentErrorCount);
    }
}
