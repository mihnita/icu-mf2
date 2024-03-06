package com.ibm.icu.message2x;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SuppressWarnings({ "static-method", "javadoc" })
public class DebugSingleTest {
    @Test
    public void test() {
        String test = "Hello";
        System.out.println(Utilities.str(test));
        MfParser.parse(test);
    }
}
