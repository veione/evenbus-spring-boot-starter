package com.think.event;

import java.lang.reflect.Method;

/**
 * Used internally by EventBus and generated subscriber indexes.
 *
 * @author veione
 */
public record SubscriberMethod(Method method, Class<?> eventType, ThreadMode threadMode, int priority, String threadPoolName) {
}