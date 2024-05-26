package com.think.event.util;

/**
 * Asm tools
 *
 * @author veione
 */
public final class AsmUtils {

    /**
     * 将以'.'分割的类名改为以'/'为分割的类名
     *
     * @param sourceName
     * @return
     */
    public static String toInternalName(String sourceName) {
        StringBuilder sb = new StringBuilder(64);
        int start = 0;
        int index = sourceName.indexOf('.', start);
        while (index != -1) {
            sb.append(sourceName, start, index).append('/');
            start = index + 1;
            index = sourceName.indexOf('.', start);
        }
        sb.append(sourceName.substring(start));
        return sb.toString();
    }

    /**
     * 将给定的Class转换为对应的字节码描述符
     *
     * @param clazz
     * @return
     */
    public static String toDescriptor(Class<?> clazz) {
        return String.format("L%s;", toInternalName(clazz.getName()));
    }
}
