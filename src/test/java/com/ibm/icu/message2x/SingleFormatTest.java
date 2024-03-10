// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2x;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SuppressWarnings({ "static-method", "javadoc" })
public class SingleFormatTest {
    static private final Map<String, Object> ARGS = new HashMap<>();
    static {
        ARGS.put("user", "John");
        ARGS.put("count", 42);
        ARGS.put("exp", new Date(2024 - 1900, 7, 15));
    }

    @Test
    public void test() {
        String[] testStrings = {
                
                ""
                        + ".input {$exp :datetime dateStyle=medium timeStyle=short}\n"
                        + ".input {$user :string}\n"
                        + ".local $longExp = {$exp :datetime dateStyle=full}"
                        + "{{Hello {$user}, you want '{$exp}' or '{$longExp}'?}}"
                // "Hello John, you want 'Aug 15, 2024, 12:00 AM' or 'Thursday, August 15, 2024 at 12:00 AM'?"
        };
        for (String test : testStrings) {
            checkOneString(test);
        }
    }

    void checkOneString(String pattern) {
        System.out.println("========================");
        System.out.println(Utilities.str(pattern));

        MFParser.debug = false;
        MessageFormatter mf = MessageFormatter.builder()
                .setLocale(Locale.US)
                .setPattern(pattern)
                .build();
        String result = mf.formatToString(ARGS);
        System.out.println("RESULT: " + result);
    }
}
