package com.think.event;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Method;

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

public class ByteCodeSubscriber {

    public static byte[] enhance(Object target, Method method) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        String className = "com/think/event/MySubscriberInvoker";
        String superName = "java/lang/Object";

        cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, className.replace(".", "/"), null, superName.replace('.', '/'), new String[] {SubscriberInvoker.class.getName().replace(".", "/")});

        {
            cw.visitField(ACC_PRIVATE + ACC_FINAL, "bus", "Lcom/think/event/EventBus;", null, null).visitEnd();
            cw.visitField(ACC_PRIVATE + ACC_FINAL, "target", "Ljava/lang/Object;", null, null).visitEnd();
            cw.visitField(ACC_PRIVATE + ACC_FINAL, "method", "Ljava/lang/reflect/Method;", null, null).visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(Lcom/think/event/EventBus;Ljava/lang/Object;Ljava/lang/reflect/Method;)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0); // this
            mv.visitVarInsn(ALOAD, 1); // EventBus
            mv.visitFieldInsn(PUTFIELD, className.replace('.', '/'), "bus", "Lcom/think/event/EventBus;");
            mv.visitVarInsn(ALOAD, 0); // this
            mv.visitVarInsn(ALOAD, 2); // Object
            mv.visitFieldInsn(PUTFIELD, className.replace('.', '/'), "target", "Ljava/lang/Object;");
            mv.visitVarInsn(ALOAD, 0); // this
            mv.visitVarInsn(ALOAD, 3); // Method
            mv.visitFieldInsn(PUTFIELD, className.replace('.', '/'), "method", "Ljava/lang/reflect/Method;");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, superName.replace('.', '/'), "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0); // 让ASM自动计算栈和局部变量表大小
            mv.visitEnd();
        }

        {
            // 定义invoke方法
            String targetClass = target.getClass().getCanonicalName().replace(".", "/");

            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "invoke", "(Ljava/lang/Object;)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "target", "Ljava/lang/Object;");
            mv.visitTypeInsn(CHECKCAST, targetClass);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, targetClass, method.getName(), "(Ljava/lang/Object;)V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }

        {
            cw.visitEnd();
        }

        byte[] classBytes = cw.toByteArray();
        return classBytes;
    }

    public static void main(String[] args) throws Exception {
        MyEventController controller = new MyEventController();

        Method method = controller.getClass().getMethod("onEvent", Object.class);
        byte[] classBytes = enhance(controller, method);
        Class<?> generatedClass = new MyClassLoader().defineClass("com.think.event.MySubscriberInvoker", classBytes);

        // 注意：这里的实例化需要你有合适的构造器参数（EventBus实例，目标对象，以及Method实例）
        Object subscriberInstance = generatedClass.getDeclaredConstructor(EventBus.class, Object.class, Method.class).newInstance(null, controller, method);
        System.out.println(subscriberInstance);
        SubscriberInvoker subscriber = (SubscriberInvoker) subscriberInstance;
        subscriber.invoke(new DeadEvent("hello", "event"));
    }

    /**
     * 自定义类加载器
     */
    public static class MyClassLoader extends ClassLoader {
        public MyClassLoader() {
            super(Thread.currentThread().getContextClassLoader());
        }

        /**
         * 将字节数组转化为Class对象
         *
         * @param name 类全名
         * @param data class数组
         * @return
         */
        public Class<?> defineClass(String name, byte[] data) {
            return this.defineClass(name, data, 0, data.length);
        }

    }
}
