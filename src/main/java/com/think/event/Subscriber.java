package com.think.event;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.Executor;

import static com.think.event.util.AsmUtils.toDescriptor;
import static com.think.event.util.AsmUtils.toInternalName;
import static org.objectweb.asm.Opcodes.ACC_BRIDGE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

/**
 * A subscriber method on a specific object, plus the executor that should be used for dispatching
 * events to it.
 *
 * <p>Two subscribers are equivalent when they refer to the same method on the same object (not
 * class). This property is used to ensure that no subscriber method is registered more than once.
 *
 * @author Colin Decker
 */
class Subscriber {

    static Subscriber create(EventBus bus, Object listener, SubscriberMethod method) {
        return new Subscriber(bus, listener, method);
    }

    /**
     * The event eventBus this subscriber belongs to.
     */
    private final EventBus bus;

    /**
     * The object with the subscriber method.
     */
    final Object subscriber;

    /**
     * Subscriber method.
     */
    final SubscriberMethod method;

    /**
     * Executor to use for dispatching events to this subscriber.
     */
    private final Executor executor;

    private final Executor customExecutor;
    /**
     * Becomes false as soon as {@link EventBus#unregister(Object)} is called, which is checked by queued event delivery
     * {@link EventBus#invokeSubscriber(PendingPost)} to prevent race conditions.
     */
    volatile boolean active;

    /**
     * Subscriber invoker.
     */
    private final SubscriberInvoker subscriberInvoker;

    private Subscriber(EventBus bus, Object subscriber, SubscriberMethod method) {
        this.bus = bus;
        this.subscriber = Objects.requireNonNull(subscriber);
        this.method = method;
        active = true;

        this.executor = bus.executor();

        if (method.threadMode() == ThreadMode.CUSTOM) {
            this.customExecutor = EventBus.getDefault().getApplicationContext().getBean(method.threadPoolName(), Executor.class);
        } else {
            this.customExecutor = null;
        }

        try {
            this.subscriberInvoker = createSubscriberInvoker();
        } catch (Exception e) {
            throw new IllegalStateException("Enhance subscriber invoke occur exception for " + subscriber.getClass().getName() + ", method " + method.method().getName(), e);
        }
    }

    private <T, E> SubscriberInvoker<T, E> createSubscriberInvoker() throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        Class<?> eventType = method.eventType();
        String className = String.format("com/think/event/SubscriberInvoker%s%s", eventType.getSimpleName(), method.method().getName());
        String superName = toInternalName(Object.class.getName());
        String interfaceName = toInternalName(SubscriberInvoker.class.getName());
        String targetName = toInternalName(subscriber.getClass().getName());
        String eventName = toInternalName(eventType.getName());
        String exceptionName = toInternalName(Exception.class.getName());

        String eventBusDescriptor = toDescriptor(EventBus.class);
        String targetDescriptor = toDescriptor(subscriber.getClass());
        String methodDescriptor = toDescriptor(Method.class);
        String eventDescriptor = toDescriptor(eventType);

        // 定义头信息
        cw.visit(V1_8, ACC_PUBLIC | ACC_SUPER | ACC_FINAL, className,
                "Ljava/lang/Object;Lcom/think/event/SubscriberInvoker<%s%s>;".formatted(targetDescriptor, eventDescriptor),
                superName, new String[]{interfaceName});

        {
            // 字段
            cw.visitField(ACC_PRIVATE + ACC_FINAL, "bus", eventBusDescriptor, null, null).visitEnd();
            cw.visitField(ACC_PRIVATE + ACC_FINAL, "target", targetDescriptor, null, null).visitEnd();
            cw.visitField(ACC_PRIVATE + ACC_FINAL, "method", methodDescriptor, null, null).visitEnd();
        }

        {
            // 构造函数
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(%s%s%s)V".formatted(eventBusDescriptor, targetDescriptor, methodDescriptor), null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(PUTFIELD, className, "bus", eventBusDescriptor);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitFieldInsn(PUTFIELD, className, "target", targetDescriptor);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitFieldInsn(PUTFIELD, className, "method", methodDescriptor);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 4);
            mv.visitEnd();
        }

        String[] exceptions = new String[]{exceptionName};

        {
            // 定义invoke方法
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "invoke", "(%s)V".formatted(eventDescriptor), null, exceptions);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "target", targetDescriptor);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, targetName, method.method().getName(), "(%s)V".formatted(eventDescriptor), false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        {
            MethodVisitor methodVisitor = cw.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC, "invoke", "(Ljava/lang/Object;)V", null, exceptions);
            methodVisitor.visitCode();
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitTypeInsn(CHECKCAST, eventName);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, className, "invoke", "(%s)V".formatted(eventDescriptor), false);
            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(2, 2);
            methodVisitor.visitEnd();
        }

        {
            cw.visitEnd();
        }

        byte[] bytes = cw.toByteArray();

//        try {
//            FileOutputStream fos = new FileOutputStream("D:\\Subscriber.class");
//            fos.write(bytes);
//            fos.close();
//            System.out.println("Byte array has been written to file successfully.");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        Class<?> invokeClass = bus.defineClass(StringUtils.replace(className, "/", "."), bytes);
        Object invokeInstance = invokeClass.getDeclaredConstructor(EventBus.class, subscriber.getClass(), Method.class).newInstance(bus, subscriber, method.method());
        return (SubscriberInvoker<T, E>) invokeInstance;
    }

    final void postToSubscription(Object event) {
        switch (method.threadMode()) {
            case POSTING -> {
                try {
                    invokeSubscriberMethod(event);
                } catch (Exception e) {
                    Throwable cause = e.getCause();
                    if (cause == null) {
                        cause = e;
                    }
                    bus.handleSubscriberException(cause, context(event));
                }
            }
            case ASYNC -> dispatchEvent(event);
            case CUSTOM -> dispatchEvent(customExecutor, event);
        }
    }

    /**
     * Dispatches {@code event} to this subscriber using the proper executor.
     */
    final void dispatchEvent(Executor executor, Object event) {
        executor.execute(
                () -> {
                    try {
                        invokeSubscriberMethod(event);
                    } catch (Exception e) {
                        bus.handleSubscriberException(e.getCause(), context(event));
                    }
                });
    }

    /**
     * Dispatches {@code event} to this subscriber using the proper executor.
     */
    final void dispatchEvent(Object event) {
        dispatchEvent(executor, event);
    }

    /**
     * Invokes the subscriber method. This method can be overridden to make the invocation
     * synchronized.
     */
    void invokeSubscriberMethod(Object event) throws Exception {
        try {
            subscriberInvoker.invoke(event);
        } catch (IllegalArgumentException e) {
            throw new Error("Method rejected target/argument: " + event, e);
        } catch (Exception e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                throw new Error("Method rejected target/argument: " + event, e);
            }
            if (e.getCause() instanceof IllegalAccessException) {
                throw new Error("Method became inaccessible: " + event, e);
            }
            if (e.getCause() instanceof Error) {
                throw (Error) e.getCause();
            }
            throw e;
        }
    }

    /**
     * Gets the context for the given event.
     */
    private SubscriberExceptionContext context(Object event) {
        return new SubscriberExceptionContext(bus, event, subscriber, method.method());
    }

    @Override
    public final int hashCode() {
        return (31 + method.hashCode()) * 31 + System.identityHashCode(subscriber);
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof Subscriber that) {
            // Use == so that different equal instances will still receive events.
            // We only guard against the case that the same object is registered
            // multiple times
            return subscriber == that.subscriber && method.equals(that.method);
        }
        return false;
    }
}
