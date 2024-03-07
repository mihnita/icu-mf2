// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2x;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SuppressWarnings({ "static-method", "javadoc" })
public class SingleFormatTest {
    @Test
    public void test() {
        String test = "Hello {$name}!";
        System.out.println(Utilities.str(test));

        MessageFormatter mf = MessageFormatter.builder()
                .setLocale(Locale.US)
                .setPattern(test)
                .build();

        Map<String, Object> args = new HashMap<>();
        String result = mf.formatToString(args);
        System.out.println("RESULT: " + result);
//        MfParser.parse(test);
    }
}
