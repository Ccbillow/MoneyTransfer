package org.example.transfer.executor;


import org.example.common.exception.BusinessException;
import org.example.common.exception.enums.ExceptionEnum;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
public class IdempotentExecutor {

//    private static Map<String, Boolean> idempotentMap = new ConcurrentHashMap<>();
//
//    public void execute(String requestId, Runnable task) {
//        execute(requestId, () -> {
//            task.run();
//            return null;
//        });
//    }
//
//    /**
//     * Executes idempotent task
//     *
//     * @param requestId idempotent key
//     * @param task      business task
//     * @return business result
//     * @throws BusinessException if duplicate request
//     */
//    public static <T> T execute(String requestId, Supplier<T> task) {
//        boolean isFirst = idempotentMap.putIfAbsent(requestId, Boolean.TRUE) == null;
//        if (!isFirst) {
//            throw new BusinessException(ExceptionEnum.IDEMPOTENT_REQUEST.getErrorCode(),
//                    String.format("Duplicate request, requestId: %s", requestId));
//        }
//
//        try {
//            return task.get();
//        } finally {
//            idempotentMap.remove(requestId);
//        }
//    }


    @Autowired
    private RedissonClient redissonClient;

    public void execute(String idempotentKey, Runnable task) {
        this.execute(idempotentKey, 3600L, task);
    }

    /**
     * Executes idempotent task (no return)
     */
    public void execute(String idempotentKey, long expireSeconds, Runnable task) {
        executeWithIdempotency(idempotentKey, expireSeconds, () -> {
            task.run();
            return null;
        });
    }

    /**
     * Executes idempotent task
     *
     * @param idempotentKey idempotent key (requestId)
     * @param expireSeconds key expire time (s)
     * @param task          business task
     * @return business result
     * @throws BusinessException if duplicate request
     */
    public <T> T executeWithIdempotency(String idempotentKey, long expireSeconds, Supplier<T> task) {
        RBucket<String> bucket = redissonClient.getBucket(idempotentKey);
        // trySet is atomic (Redis SET NX) — prevents race condition between check and set
        boolean acquired = bucket.trySet("PROCESSING", expireSeconds, TimeUnit.SECONDS);
        if (!acquired) {
            throw new BusinessException(ExceptionEnum.IDEMPOTENT_REQUEST.getErrorCode(),
                    String.format("Duplicate request, requestId: %s", idempotentKey));
        }
        try {
            T result = task.get();
            bucket.set("DONE", expireSeconds, TimeUnit.SECONDS);
            return result;
        } catch (Exception e) {
            // release key on failure so the caller can retry with the same requestId
            bucket.delete();
            throw e;
        }
    }
}