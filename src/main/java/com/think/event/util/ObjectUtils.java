package com.think.event.util;

import java.util.Optional;

public final class ObjectUtils {
    /**
     * Returns the first of two given parameters that is not {@code null}, if either is, or otherwise
     * throws a {@link NullPointerException}.
     *
     * <p>To find the first non-null element in an iterable, use {@code Iterables.find(iterable,
     * Predicates.notNull())}. For varargs, use {@code Iterables.find(Arrays.asList(a, b, c, ...),
     * Predicates.notNull())}, static importing as necessary.
     *
     * <p><b>Note:</b> if {@code first} is represented as an {@link Optional}, this can be
     * accomplished with {@link Optional#or(Object) first.or(second)}. That approach also allows for
     * lazy evaluation of the fallback instance, using {@link Optional#or(Supplier)
     * first.or(supplier)}.
     *
     * <p><b>Java 9 users:</b> use {@code java.util.Objects.requireNonNullElse(first, second)}
     * instead.
     *
     * @return {@code first} if it is non-null; otherwise {@code second} if it is non-null
     * @throws NullPointerException if both {@code first} and {@code second} are null
     * @since 18.0 (since 3.0 as {@code Objects.firstNonNull()}).
     */
    public static <T> T firstNonNull(T first, T second) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        throw new NullPointerException("Both parameters are null");
    }
}
