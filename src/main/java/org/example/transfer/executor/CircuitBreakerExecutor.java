package org.example.transfer.executor;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.example.transfer.comm.enums.ExceptionEnum;
import org.example.transfer.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class CircuitBreakerExecutor {

    private CircuitBreaker circuitBreaker;

    public CircuitBreakerExecutor(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("transferCircuitBreaker");
    }

    public void execute(Runnable task) {
        execute(() -> {
            task.run();
            return null;
        });
    }

    /**
     * Executes the given task within a circuit breaker context.
     * If the circuit is open, the call is skipped to protect the system.
     *
     * @param task business task
     * @return business result
     */
    public <T> T execute(Supplier<T> task) {
        try {
            return CircuitBreaker.decorateSupplier(circuitBreaker, task).get();
        } catch (CallNotPermittedException e) {
            throw new BusinessException(ExceptionEnum.CIRCUIT_OPEN.getErrorCode(),
                    ExceptionEnum.CIRCUIT_OPEN.getErrorMsg());
        }
    }
}