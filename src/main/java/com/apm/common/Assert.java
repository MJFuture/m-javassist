package com.apm.common;

/**
 */
public class Assert {
    public static void checkNull(Object obj) {
        if (obj == null)
            throw new AssertionError();
    }

    public static void checkNull(Object obj, String msg) {
        if (obj == null)
            throw new AssertionError(msg);
    }

}
