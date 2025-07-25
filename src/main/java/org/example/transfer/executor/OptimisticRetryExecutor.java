package org.example.transfer.executor;

import jakarta.persistence.OptimisticLockException;
import org.example.transfer.comm.enums.ExceptionEnum;
import org.example.transfer.exception.BusinessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.function.Supplier;

/**
 * transfer retry
 */
@Component
public class OptimisticRetryExecutor {

    // default retry time
    // todo maintain it in ConfigCenter
    private static final int DEFAULT_MAX_RETRIES = 3;

    public void executeWithRetry(Runnable task) {
        executeWithRetry(() -> {
            task.run();
            return null;
        }, DEFAULT_MAX_RETRIES);
    }

    /**
     * Executes a task with retries on optimistic lock exceptions.
     *
     * @param task       business task
     * @param maxRetries max retry times
     * @return business result
     * @throws BusinessException if retries exceed maxRetries
     */
    public <T> T executeWithRetry(Supplier<T> task, int maxRetries) {
        int retry = 0;
        Random random = new Random();
        while (true) {
            try {
                return task.get();
            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                retry++;
                System.out.println("retry:" + retry);
                if (retry > maxRetries) {
                    //todo 1. save to error table; 2. send email to developer
                    throw new BusinessException(ExceptionEnum.OPTIMISTIC_LOCK_MAX_RETRY_ERROR.getErrorCode(),
                            ExceptionEnum.OPTIMISTIC_LOCK_MAX_RETRY_ERROR.getErrorMsg());
                }

                try {
                    // Random retry wait time, improve concurrency
                    Thread.sleep(100L * retry + random.nextInt(200));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
    }
}