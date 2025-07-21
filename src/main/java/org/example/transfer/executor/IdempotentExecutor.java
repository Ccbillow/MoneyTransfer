package org.example.transfer.executor;


//@Component
public class IdempotentExecutor {

//    @Autowired
//    private RedissonClient redissonClient;
//
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