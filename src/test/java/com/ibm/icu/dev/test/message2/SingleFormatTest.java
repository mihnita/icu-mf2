// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.message2x.MessageFormatter;

@RunWith(JUnit4.class)
@SuppressWarnings({ "static-method", "javadoc" })
public class SingleFormatTest {
    static private final Map<String, Object> ARGS = new HashMap<>();
    static {
        ARGS.put("user", "John");
        ARGS.put("count", 42);
        ARGS.put("exp", new Date(2024 - 1900, 7, 3, 21, 43, 57)); // Aug 3, 2024, at 9:43:57 pm
        ARGS.put("tsOver", "full");
    }

    @Test
    public void test() {
        String[] testStrings = {
                ""
                        + ".input {$exp :datetime timeStyle=short}\n"
                        + ".input {$user :string}\n"
                        + ".local $longExp = {$exp :datetime dateStyle=long}"
                        + ".local $zooExp = {$exp :datetime dateStyle=short timeStyle=$tsOver}"
                        + "{{Hello John, you want '{$exp}', '{$longExp}', or '{$zooExp}' or even '{$exp :datetime dateStyle=full}'?}}",
                        // RESULT: "Hello John, you want '9:43 PM', 'August 3, 2024 at 9:43 PM', or '8/3/24, 9:43:57 PM Pacific Daylight Time' or even 'Saturday, August 3, 2024 at 9:43 PM'?"
                ""
                        + ".input {$exp :datetime year=numeric month=numeric day=|2-digit|}\n"
                        + ".local $longExp = {$exp :datetime month=long weekday=long}"
                        + "{{Expires on '{$exp}' ('{$longExp}').}}"
                        // RESULT: "Expires on '8/03/2024' ('Saturday, August 03, 2024')."
        };
        for (String test : testStrings) {
            checkOneString(test);
        }
    }

    void checkOneString(String pattern) {
        System.out.println("========================");
        System.out.println(Utilities.str(pattern));

        MessageFormatter mf = MessageFormatter.builder()
                .setLocale(Locale.US)
                .setPattern(pattern)
                .build();
        String result = mf.formatToString(ARGS);
        System.out.println("RESULT: " + result);
    }
}
