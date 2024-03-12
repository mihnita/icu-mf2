// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import static org.junit.Assert.assertEquals;

import com.ibm.icu.message2x.MessageFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SuppressWarnings({"static-method", "javadoc"})
public class DateFormatTest {
    private static final Map<String, Object> ARGS = new HashMap<>();

    static {
        ARGS.put("user", "John");
        ARGS.put("exp", new Date(2024 - 1900, 7, 3, 21, 43, 57)); // Aug 3, 2024, at 9:43:57 pm
        ARGS.put("tsOver", "full");
    }

    @Test
    public void test() {
        String[] testStrings = {
            "Expires on {$exp}",
            "Expires on 8/3/24, 9:43 PM", // expected
            "Expires on {$exp :datetime}",
            "Expires on 8/3/24, 9:43 PM", // expected
            "Expires on {$exp :datetime icu:skeleton=yMMMMdjmsSSEE}",
            "Expires on Sat, August 3, 2024 at 9:43:57.00 PM", // expected
            "Expires on {$exp :datetime dateStyle=full}",
            "Expires on Saturday, August 3, 2024", // expected
            "Expires on {$exp :datetime dateStyle=long}",
            "Expires on August 3, 2024", // expected
            "Expires on {$exp :datetime dateStyle=medium}",
            "Expires on Aug 3, 2024", // expected
            "Expires on {$exp :datetime timeStyle=long}",
            "Expires on 9:43:57 PM PDT", // expected
            "Expires on {$exp :datetime timeStyle=medium}",
            "Expires on 9:43:57 PM", // expected
            "Expires on {$exp :datetime timeStyle=short}",
            "Expires on 9:43 PM", // expected
            "Expires on {$exp :datetime dateStyle=full timeStyle=medium}",
            "Expires on Saturday, August 3, 2024 at 9:43:57 PM", // expected
            "Expires on {$exp :datetime year=numeric month=long}",
            "Expires on August 2024", // expected
            "Expires on {$exp :datetime year=numeric month=medium day=numeric weekday=long hour=numeric minute=numeric}",
            "Expires on 3 Saturday 2024, 9:43 PM", // expected
            // Make sure we ignore date / time fields if needed
            "Expires on {$exp :date year=numeric month=medium day=numeric weekday=long hour=numeric minute=numeric}",
            "Expires on 3 Saturday 2024", // expected
            "Expires at {$exp :time year=numeric month=medium day=numeric weekday=long hour=numeric minute=numeric}",
            "Expires at 9:43 PM", // expected
            "Expires at {$exp :time style=long dateStyle=full timeStyle=medium}",
            "Expires at 9:43:57 PM PDT", // expected
            "Expires on {$exp :date style=long dateStyle=full timeStyle=medium}",
            "Expires on August 3, 2024", // expected
            // Literals
            "Expires on {|2025-02-27| :datetime dateStyle=full}",
            "Expires on Thursday, February 27, 2025", // expected
            "Expires at {|19:23:45| :datetime timeStyle=full}",
            "Expires at 7:23:45 PM Pacific Daylight Time", // expected
            "Expires at {|19:23:45.123| :datetime timeStyle=full}",
            "Expires at 7:23:45 PM Pacific Daylight Time", // expected
            "Expires on {|2025-02-27T19:23:45| :datetime dateStyle=full}",
            "Expires on Thursday, February 27, 2025", // expected
            // TODO: better ISO 8601 parsing
            "Expires at {|19:23:45Z| :datetime timeStyle=full}",
            "Expires at {|19:23:45Z|}", // expected
            "Expires at {|19:23:45+03:30:00| :datetime timeStyle=full}",
            "Expires at {|19:23:45+03:30:00|}", // expected
            // Chaining
            ""
                    + ".input {$exp :datetime timeStyle=short}\n"
                    + ".input {$user :string}\n"
                    + ".local $longExp = {$exp :datetime dateStyle=long}"
                    + ".local $zooExp = {$exp :datetime dateStyle=short timeStyle=$tsOver}"
                    + "{{Hello John, you want '{$exp}', '{$longExp}', or '{$zooExp}' or even '{$exp :datetime dateStyle=full}'?}}",
            "Hello John, you want '9:43 PM', 'August 3, 2024 at 9:43 PM', or '8/3/24, 9:43:57 PM Pacific Daylight Time' or even 'Saturday, August 3, 2024 at 9:43 PM'?",
            ""
                    + ".input {$exp :datetime year=numeric month=numeric day=|2-digit|}\n"
                    + ".local $longExp = {$exp :datetime month=long weekday=long}"
                    + "{{Expires on '{$exp}' ('{$longExp}').}}",
            "Expires on '8/03/2024' ('Saturday, August 03, 2024').",
        };
        for (int i = 0; i < testStrings.length; i += 2) {
            checkOneString(testStrings[i], testStrings[i + 1]);
        }
    }

    void checkOneString(String pattern, String expected) {
        MessageFormatter mf =
                MessageFormatter.builder().setLocale(Locale.US).setPattern(pattern).build();
        String result = mf.formatToString(ARGS);
        if (!expected.isEmpty()) {
            assertEquals(expected, result);
        } else {
            System.out.println("GOT : '" + result + "'");
        }
    }
}
