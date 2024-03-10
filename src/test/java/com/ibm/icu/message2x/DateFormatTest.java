// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2x;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SuppressWarnings({ "static-method", "javadoc" })
public class DateFormatTest {
    static private final Map<String, Object> ARGS = new HashMap<>();
    static {
        ARGS.put("exp", new Date(2024 - 1900, 7, 15, 19, 42, 58));
    }

    @Test
    public void test() {
        String[] testStrings = {
                "Expires on {$exp}",
                "Expires on 8/15/24, 7:42 PM", // expected
                "Expires on {$exp :datetime}",
                "Expires on 8/15/24, 7:42 PM", // expected
                "Expires on {$exp :datetime icu:skeleton=yMMMMdjmsSSEE}",
                "Expires on Thu, August 15, 2024 at 7:42:58.00 PM", // expected
                "Expires on {$exp :datetime dateStyle=full}",
                "Expires on Thursday, August 15, 2024", // expected
                "Expires on {$exp :datetime dateStyle=long}",
                "Expires on August 15, 2024", // expected
                "Expires on {$exp :datetime dateStyle=medium}",
                "Expires on Aug 15, 2024", // expected
                "Expires on {$exp :datetime timeStyle=long}",
                "Expires on 7:42:58 PM PDT", // expected
                "Expires on {$exp :datetime timeStyle=medium}",
                "Expires on 7:42:58 PM", // expected
                "Expires on {$exp :datetime timeStyle=short}",
                "Expires on 7:42 PM", // expected
                "Expires on {$exp :datetime dateStyle=full timeStyle=medium}",
                "Expires on Thursday, August 15, 2024 at 7:42:58 PM", // expected
                "Expires on {$exp :datetime year=numeric month=long}",
                "Expires on August 2024", // expected
                "Expires on {$exp :datetime year=numeric month=medium day=numeric weekday=long hour=numeric minute=numeric}",
                "Expires on 2024 (day: 15), 7:42 PM", // expected
                // Literals
                "Expires on {|2025-02-27| :datetime dateStyle=full}", "Expires on Thursday, March 27, 2025",
                "Expires at {|19:23:45| :datetime timeStyle=full}", "Expires at 7:23:45 PM Pacific Daylight Time",
                "Expires at {|19:23:45.123| :datetime timeStyle=full}", "Expires at 7:23:45 PM Pacific Daylight Time",
                "Expires on {|2025-02-27T19:23:45| :datetime dateStyle=full}", "Expires on Thursday, March 27, 2025",
                "Expires at {|19:23:45Z| :datetime timeStyle=full}", "Expires at 19:23:45Z",
                "Expires at {|19:23:45+03:30:00| :datetime timeStyle=full}", "Expires at 19:23:45+03:30:00",
        };
        for (int i = 0; i < testStrings.length; i += 2) {
            checkOneString(testStrings[i], testStrings[i + 1]);
        }
    }

    void checkOneString(String pattern, String expected) {
        MessageFormatter mf = MessageFormatter.builder()
                .setLocale(Locale.US)
                .setPattern(pattern)
                .build();
        String result = mf.formatToString(ARGS);
        if (!expected.isEmpty())
            assertEquals(expected, result);
        else
            System.out.println("GOT : '" + result + "'");
    }
}
