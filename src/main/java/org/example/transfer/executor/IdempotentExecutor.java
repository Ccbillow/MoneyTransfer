package org.example.transfer.executor;


import org.example.transfer.comm.enums.ExceptionEnum;
import org.example.transfer.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class IdempotentExecutor {

    private static Map<String, Boolean> idempotentMap = new ConcurrentHashMap<>();

    public void execute(String requestId, Runnable task) {
        execute(requestId, () -> {
            task.run();
            return null;
        });
    }

    /**
     * Executes idempotent task
     *
     * @param requestId idempotent key
     * @param task      business task
     * @return business result
     * @throws BusinessException if duplicate request
     */
    public static <T> T execute(String requestId, Supplier<T> task) {
        boolean isFirst = idempotentMap.putIfAbsent(requestId, Boolean.TRUE) == null;
        if (!isFirst) {
            throw new BusinessException(ExceptionEnum.IDEMPOTENT_REQUEST.getErrorCode(),
                    String.format("Duplicate request, requestId: %s", requestId));
        }

        try {
            return task.get();
        } finally {
            idempotentMap.remove(requestId);
        }
    }


    //    @Autowired
//    private RedissonClient redissonClient;


//    /**
//     * Executes idempotent task (no return)
//     */
//    public void executeWithIdempotency(String idempotentKey, long expireSeconds, Runnable task) {
//        executeWithIdempotency(idempotentKey, expireSeconds, () -> {
//            task.run();
//            return null;
//        });
//    }
//
//    /**
//     * Executes idempotent task
//     *
//     * @param idempotentKey idempotent key (requestId)
//     * @param expireSeconds key expire time (s)
//     * @param task          business task
//     * @return business result
//     * @throws BusinessException if duplicate request
//     */
//    public <T> T executeWithIdempotency(String idempotentKey, long expireSeconds, Supplier<T> task) {
//        RBucket<String> bucket = redissonClient.getBucket(idempotentKey);
//        if (bucket.isExists()) {
//            throw new BusinessException(ExceptionEnum.IDEMPOTENT_REQUEST.getErrorCode(),
//                    String.format("Duplicate request, requestId: %s", idempotentKey));
//        }
//
//        T result = task.get();
//
//        bucket.set("DONE", expireSeconds, TimeUnit.SECONDS);
//        return result;
//    }
}