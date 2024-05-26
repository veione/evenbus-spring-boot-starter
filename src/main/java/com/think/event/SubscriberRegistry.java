package com.think.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry of subscribers to a single event eventBus.
 *
 * @author veione
 */
final class SubscriberRegistry {
    private final ConcurrentMap<Class<?>, CopyOnWriteArrayList<Subscriber>> subscribers = new ConcurrentHashMap<>();
    private final Map<Object, List<Class<?>>> typesBySubscriber = new HashMap<>();
    private static final ConcurrentMap<Class<?>, List<Class<?>>> HIERARCHY_CACHE = new ConcurrentHashMap<>();
    private final EventBus bus;
    private final SubscriberMethodFinder subscriberMethodFinder = new SubscriberMethodFinder();

    SubscriberRegistry(EventBus bus) {
        this.bus = Objects.requireNonNull(bus);
    }

    /**
     * Registers all subscriber methods on the given listener object.
     *
     * @param listener
     */
    void register(Object listener) {
        Class<?> subscriberClass = listener.getClass();
        List<SubscriberMethod> subscriberMethods = subscriberMethodFinder.findSubscriberMethods(subscriberClass);

        synchronized (this) {
            for (SubscriberMethod subscriberMethod : subscriberMethods) {
                subscribe(listener, subscriberMethod);
            }
        }
    }

    // Must be called in synchronized block
    private void subscribe(Object subscriber, SubscriberMethod subscriberMethod) {
        Class<?> eventType = subscriberMethod.eventType();
        Subscriber newSubscription = Subscriber.create(bus, subscriber, subscriberMethod);
        CopyOnWriteArrayList<Subscriber> subscriptions = subscribers.get(eventType);
        if (subscriptions == null) {
            subscriptions = new CopyOnWriteArrayList<>();
            subscribers.put(eventType, subscriptions);
        } else {
            if (subscriptions.contains(newSubscription)) {
                throw new EventBusException("Subscriber " + subscriber.getClass() + " already registered to event "
                        + eventType);
            }
        }

        int size = subscriptions.size();
        for (int i = 0; i <= size; i++) {
            if (i == size || subscriberMethod.priority() > subscriptions.get(i).method.priority()) {
                subscriptions.add(i, newSubscription);
                break;
            }
        }

        List<Class<?>> subscribedEvents = typesBySubscriber.computeIfAbsent(subscriber, k -> new ArrayList<>());
        subscribedEvents.add(eventType);
    }

    /** Only updates subscriptionsByEventType, not typesBySubscriber! Caller must update typesBySubscriber. */
    private void unsubscribeByEventType(Object subscriber, Class<?> eventType) {
        List<Subscriber> subscriptions = subscribers.get(eventType);
        if (subscriptions != null) {
            int size = subscriptions.size();
            for (int i = 0; i < size; i++) {
                Subscriber subscription = subscriptions.get(i);
                if (subscription.subscriber == subscriber) {
                    subscription.active = false;
                    subscriptions.remove(i);
                    i--;
                    size--;
                }
            }
        }
    }

    /**
     * Unregister all subscribers on the given listener object.
     *
     * @param listener
     */
    void unregister(Object listener) {
        List<Class<?>> subscribedTypes = typesBySubscriber.get(listener);
        if (subscribedTypes != null) {
            for (Class<?> eventType : subscribedTypes) {
                unsubscribeByEventType(listener, eventType);
            }
            typesBySubscriber.remove(listener);
        } else {
            throw new IllegalArgumentException(
                    "missing event subscriber for an annotated method. Is " + listener + " registered?");
        }
    }

    /**
     * Gets an iterator representing an immutable snapshot of all subscribers to the given event at
     * the time this method is called.
     */
    Iterator<Subscriber> getSubscribers(Object event) {
        List<Class<?>> eventTypes = flattenHierarchy(event.getClass());

        List<Subscriber> subscriberIterators = new ArrayList<>(eventTypes.size());

        for (Class<?> eventType : eventTypes) {
            CopyOnWriteArrayList<Subscriber> eventSubscribers = subscribers.get(eventType);
            if (eventSubscribers != null) {
                subscriberIterators.addAll(eventSubscribers);
            }
        }

        return subscriberIterators.iterator();
    }

    /**
     * Flattens a class's type hierarchy into a set of {@code Class} objects including all
     * superclasses (transitively) and all interfaces implemented by these superclasses.
     */
    static List<Class<?>> flattenHierarchy(Class<?> clazz) {
        List<Class<?>> hierarchyClasses = HIERARCHY_CACHE.get(clazz);
        if (hierarchyClasses == null) {
            Class<?> concreteClass = clazz;
            hierarchyClasses = new ArrayList<>(4);
            while (concreteClass != null) {
                hierarchyClasses.add(concreteClass);
                concreteClass = concreteClass.getSuperclass();
            }
            HIERARCHY_CACHE.put(clazz, hierarchyClasses);
        }
        return hierarchyClasses;
    }
}
