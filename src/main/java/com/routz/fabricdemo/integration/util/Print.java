package com.routz.fabricdemo.integration.util;

import static java.lang.String.format;

public class Print {
    public static void out(String format, Object... args) {
        System.err.flush();
        System.out.flush();
        System.out.println(format(format, args));
        System.err.flush();
        System.out.flush();
    }

    public static void fail(String msg) {
        out(msg, null);
    }

    public static void assertEquals(Object o1, Object o2) {
        assertEquals(o1 + " not equals" + o2, o1, o2);
    }

    public static void assertEquals(String msg, Object o1, Object o2) {
        if (!o1.equals(o2)) throw new IllegalArgumentException(msg);
    }

    public static void assertNotNull(Object o1) {
        if (o1 == null) {
            throw new NullPointerException(":P");
        }
    }

    public static void assertNull(Object o1) {
        if (o1 != null) {
            throw new NullPointerException(":P");
        }
    }

    public static void assertTrue(boolean b) {
        if (!b) {
            throw new IllegalArgumentException("你这爱情的骗子");
        }
    }

    public static void assertFalse(boolean b) {
        assertTrue(!b);
    }

    //CHECKSTYLE.ON: Method length is 320 lines (max allowed is 150).

    public static String printableString(final String string) {
        int maxLogStringLength = 64;
        if (string == null || string.length() == 0) {
            return string;
        }

        String ret = string.replaceAll("[^\\p{Print}]", "?");
        ret = ret.substring(0, Math.min(ret.length(), maxLogStringLength)) + (ret.length() > maxLogStringLength ? "..." : "");
        return ret;
    }
}
