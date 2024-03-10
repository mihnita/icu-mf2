// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.message2x.MFParser;

@RunWith(JUnit4.class)
@SuppressWarnings({ "static-method", "javadoc" })
public class DebugSingleTest {
    @Test
    public void test() throws Exception {
        String test = "Hello";
        System.out.println(Utilities.str(test));
        MFParser.parse(test);
    }
}