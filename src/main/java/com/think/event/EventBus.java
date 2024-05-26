package com.think.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Dispatches events to listeners, and provides ways for listeners to register themselves.
 */
public class EventBus {
    private static final Logger logger = LoggerFactory.getLogger(EventBus.class.getName());

    private final String identifier;
    private final SubscriberExceptionHandler exceptionHandler;

    private final ExecutorService executor = new ThreadPoolExecutor(4, Runtime.getRuntime().availableProcessors(),
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10000), new DefaultThreadFactory("EventBus"), new ThreadPoolExecutor.CallerRunsPolicy());

    private final SubscriberRegistry subscribers = new SubscriberRegistry(this);
    private final Dispatcher dispatcher;

    private final SubscriberClassLoader classLoader = new SubscriberClassLoader();

    static volatile EventBus defaultInstance;

    ApplicationContext applicationContext;

    public static EventBus getDefault() {
        if (defaultInstance == null) {
            synchronized (EventBus.class) {
                if (defaultInstance == null) {
                    defaultInstance = new EventBus();
                }
            }
        }
        return defaultInstance;
    }

    /**
     * Creates a new EventBus named "default".
     */
    public EventBus() {
        this("default");
    }

    public EventBus(String identifier) {
        this(
                identifier,
                Dispatcher.defaultDispatchQueue(),
                LoggingHandler.INSTANCE);
    }

    /**
     * Creates a new EventBus with the given {@link SubscriberExceptionHandler}.
     *
     * @param exceptionHandler Handler for subscriber exceptions.
     * @since 16.0
     */
    public EventBus(SubscriberExceptionHandler exceptionHandler) {
        this(
                "default",
                Dispatcher.defaultDispatchQueue(),
                exceptionHandler);
    }

    EventBus(
            String identifier,
            Dispatcher dispatcher,
            SubscriberExceptionHandler exceptionHandler) {
        this.identifier = Objects.requireNonNull(identifier);
        this.dispatcher = Objects.requireNonNull(dispatcher);
        this.exceptionHandler = Objects.requireNonNull(exceptionHandler);
    }

    /**
     * Returns the identifier for this event eventBus.
     *
     * @return
     */
    public final String identifier() {
        return identifier;
    }

    /**
     * Returns the default executor this event eventBus uses for dispatching events to subscribers.
     */
    final Executor executor() {
        return executor;
    }

    /**
     * Handles the given exception thrown by a subscriber with the given context.
     */
    void handleSubscriberException(Throwable e, SubscriberExceptionContext context) {
        Objects.requireNonNull(e);
        Objects.requireNonNull(context);

        try {
            exceptionHandler.handleException(e, context);
        } catch (Throwable e2) {
            // if the handler threw an exception... well, just log it
            logger.error(String.format("Exception %s thrown while handling exception: %s", e2, e));
        }
    }

    /**
     * Registers all subscriber methods on {@code object} to receive events.
     *
     * @param object object whose subscriber methods should be registered.
     */
    public void register(Object object) {
        subscribers.register(object);
    }

    /**
     * Unregisters all subscriber methods on a registered object.
     *
     * @param object object whose subscriber methods should be unregistered.
     * @throws IllegalArgumentException If the object was not previously registered.
     */
    public void unregister(Object object) {
        subscribers.unregister(object);
    }

    /**
     * Posts an event to all registered subscribers. This method will return successfully after the
     * event has been posted to all subscribers, and regardless of any exceptions thrown by
     * subscribers.
     *
     * <p>If no subscribers have been subscribed for {@code event}'s class, and {@code event} is not
     * already a {@link DeadEvent}, it will be wrapped in a DeadEvent and reposted.
     *
     * @param event event to post.
     */
    public void post(Object event) {
        Objects.requireNonNull(event);
        Iterator<Subscriber> eventSubscribers = subscribers.getSubscribers(event);
        if (eventSubscribers.hasNext()) {
            dispatcher.dispatch(event, eventSubscribers);
        } else if (!(event instanceof DeadEvent)) {
            // the event had no subscribers and was not itself a DeadEvent
            post(new DeadEvent(this, event));
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + identifier + ")";
    }

    public Class<?> defineClass(String name, byte[] bytes) {
        return classLoader.defineClass(name, bytes);
    }

    void shutdown() {
        executor.shutdown();
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Simple logging handler for subscriber exceptions.
     */
    static final class LoggingHandler implements SubscriberExceptionHandler {
        static final EventBus.LoggingHandler INSTANCE = new EventBus.LoggingHandler();

        @Override
        public void handleException(Throwable exception, SubscriberExceptionContext context) {
            java.util.logging.Logger logger = logger(context);
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, message(context), exception);
            }
        }

        private static java.util.logging.Logger logger(SubscriberExceptionContext context) {
            return java.util.logging.Logger.getLogger(EventBus.class.getName() + "." + context.eventBus().identifier());
        }

        private static String message(SubscriberExceptionContext context) {
            Method method = context.subscriberMethod();
            return "Exception thrown by subscriber method "
                    + method.getName()
                    + '('
                    + method.getParameterTypes()[0].getName()
                    + ')'
                    + " on subscriber "
                    + context.subscriber()
                    + " when dispatching event: "
                    + context.event();
        }
    }
}
