package org.example.executor;

/**
 * redisson redlock
 * distributed lock
 */
//@Component
public class RedisLockExecutor {

//    @Autowired
//    private RedissonClient redissonClient;
//
//    /**
//     * Executes a task protected by a Redis distributed lock.
//     *
//     * @param key      redis lock key
//     * @param waitSec  max time to wait for acquiring the lock (seconds)
//     * @param leaseSec lease time for the lock before automatic release (seconds)
//     * @param task     business task
//     * @throws RuntimeException if the lock cannot be acquired or thread is interrupted
//     */
//    public void executeWithLock(String key, int waitSec, int leaseSec, Runnable task) {
//        RLock lock = redissonClient.getLock(key);
//        boolean locked = false;
//        try {
//            locked = lock.tryLock(waitSec, leaseSec, TimeUnit.SECONDS);
//            if (!locked) {
//                throw new RuntimeException("Could not acquire Redis lock: " + key);
//            }
//            task.run();
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            throw new RuntimeException("Interrupted while acquiring Redis lock", e);
//        } finally {
//            if (locked && lock.isHeldByCurrentThread()) {
//                lock.unlock();
//            }
//        }
//    }
}