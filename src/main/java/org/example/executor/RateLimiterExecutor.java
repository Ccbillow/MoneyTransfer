package org.example.executor;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.example.comm.enums.ExceptionEnum;
import org.example.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class RateLimiterExecutor {

    private RateLimiter rateLimiter;

    public RateLimiterExecutor(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiter = rateLimiterRegistry.rateLimiter("transferRateLimiter");
    }

    public void execute(Runnable task) {
        execute(() -> {
            task.run();
            return null;
        });
    }

    /**
     * Executes the given task if permitted by the rate limiter.
     * Throws an exception if the request is not allowed due to rate limits.
     *
     * @param task business task
     * @return business result
     */
    public <T> T execute(Supplier<T> task) {
        try {
            return RateLimiter.decorateSupplier(rateLimiter, task).get();
        } catch (RequestNotPermitted e) {
            throw new BusinessException(ExceptionEnum.RATE_LIMIT_EXCEEDED.getErrorCode(),
                    ExceptionEnum.RATE_LIMIT_EXCEEDED.getErrorMsg());
        }
    }
}