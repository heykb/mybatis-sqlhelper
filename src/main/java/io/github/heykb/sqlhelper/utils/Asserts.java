package io.github.heykb.sqlhelper.utils;

public class Asserts {
    public Asserts() {
    }

    public static void check(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void check(boolean expression, String message, Object... args) {
        if (!expression) {
            throw new IllegalArgumentException(String.format(message, args));
        }
    }

    public static void check(boolean expression, String message, Object arg) {
        if (!expression) {
            throw new IllegalArgumentException(String.format(message, arg));
        }
    }

    public static void notNull(Object object, String name) {
        if (object == null) {
            throw new IllegalArgumentException(name + " is null");
        }
    }
}
