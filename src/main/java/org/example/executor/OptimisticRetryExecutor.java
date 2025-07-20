package org.example.executor;

import jakarta.persistence.OptimisticLockException;
import org.example.comm.enums.ExceptionEnum;
import org.example.exception.BusinessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * transfer retry
 */
@Component
public class OptimisticRetryExecutor {

    private static final int DEFAULT_MAX_RETRIES = 3;

    public void executeWithRetry(Runnable task) {
        executeWithRetry(() -> {
            task.run();
            return null;
        }, DEFAULT_MAX_RETRIES);
    }

    public <T> T executeWithRetry(Supplier<T> task, int maxRetries) {
        int retry = 0;
        while (true) {
            try {
                return task.get();
            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                retry++;
                System.out.println(Thread.currentThread().getName() + " retry:" + retry);
                if (retry > maxRetries) {
                    System.out.println(Thread.currentThread().getName() + " retry > maxRetries, throw max retry exception retry:" + retry);
                    throw new BusinessException(ExceptionEnum.OPTIMISTIC_LOCK_MAX_RETRY_ERROR.getErrorCode(),
                            ExceptionEnum.OPTIMISTIC_LOCK_MAX_RETRY_ERROR.getErrorMsg());
                }
                try {
                    Thread.sleep(100L * retry);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
    }
}