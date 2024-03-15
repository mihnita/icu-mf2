package com.ibm.icu.dev.test;

@SuppressWarnings("javadoc")
public class CoreTestFmwk {
    public static void assertEquals(String message, Object expected, Object actual) {
        org.junit.Assert.assertEquals(message, expected, actual);
    }
}
