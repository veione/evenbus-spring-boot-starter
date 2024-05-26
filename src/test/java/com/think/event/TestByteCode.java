package com.think.event;

import com.think.event.handler.MyEventController;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Method;

import static com.think.event.util.AsmUtils.toDescriptor;
import static com.think.event.util.AsmUtils.toInternalName;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

public class TestByteCode {

    public static byte[] enhance(Object target, Method method) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        String className = "com/think/event/MySubscriberInvoker";
        String superName = toInternalName(Object.class.getName());
        String interfaceName = toInternalName(SubscriberInvoker.class.getName());
        String targetName = toInternalName(target.getClass().getName());

        String eventBusDescriptor = toDescriptor(EventBus.class);
        String targetDescriptor = toDescriptor(Object.class);
        String methodDescriptor = toDescriptor(Method.class);

        // 定义头信息
        cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER + ACC_FINAL, className, null, superName, new String[] {interfaceName});

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

        {
            // 定义invoke方法
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "invoke", "(Ljava/lang/Object;)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "target", "Ljava/lang/Object;");
            mv.visitTypeInsn(CHECKCAST, targetName);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, targetName, method.getName(), "(Ljava/lang/Object;)V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }

        {
            cw.visitEnd();
        }

        return cw.toByteArray();
    }

    public static void main(String[] args) throws Exception {
        MyEventController target = new MyEventController();

        Method method = target.getClass().getMethod("onEvent", Object.class);
        byte[] classBytes = enhance(target, method);
        Class<?> generatedClass = new SubscriberClassLoader().defineClass("com.think.event.MySubscriberInvoker", classBytes);

        // 注意：这里的实例化需要你有合适的构造器参数（EventBus实例，目标对象，以及Method实例）
        Object subscriberInstance = generatedClass.getDeclaredConstructor(EventBus.class, Object.class, Method.class).newInstance(null, target, method);
        System.out.println(subscriberInstance);
        SubscriberInvoker subscriber = (SubscriberInvoker) subscriberInstance;
        subscriber.invoke(new DeadEvent("hello", "event"));
    }
}
