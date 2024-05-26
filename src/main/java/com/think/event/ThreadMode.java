package com.think.event;

/**
 * 事件分发处理的线程模式枚举
 *
 * @author veione
 */
public enum ThreadMode {
    /**
     * This is the default. Subscriber will be called directly in the same thread, which is posting the event. Event delivery
     * implies the least overhead because it avoids thread switching completely. Thus, this is the recommended mode for
     * simple tasks that are known to complete in a very short time without requiring the main thread. Event handlers
     * using this mode must return quickly to avoid blocking the posting thread, which may be the main thread.
     */
    POSTING,
    /**
     * Subscriber will be called in a separate thread. This is always independent of the posting thread and the
     * main thread. Posting events never wait for subscriber methods using this mode. Subscriber methods should
     * use this mode if their execution might take some time, e.g. for network access. Avoid triggering a large number
     * of long-running asynchronous subscriber methods at the same time to limit the number of concurrent threads. EventBus
     * uses a thread pool to efficiently reuse threads from completed asynchronous subscriber notifications.
     */
    ASYNC,
    /**
     * Custom thread pool.
     */
    CUSTOM
}
