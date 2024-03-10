// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.message2x.MFParser;
import com.ibm.icu.message2x.MessageFormatter;

@RunWith(JUnit4.class)
@SuppressWarnings({ "static-method", "javadoc" })
public class NumberFormatTest {
    static private final Map<String, Object> ARGS = new HashMap<>();
    static {
        ARGS.put("nrVal", 31);
    }

    static final TestCase[] TEST_CASES = {
            new TestCase.Builder()
                .pattern("Format {$val} number")
                .arguments(Args.of("val", 31))
                .expected("Format 31 number")
                .build(),
            new TestCase.Builder()
                .pattern("Format {123456789.9876} number")
                .locale("en-IN")
                .expected("Format 12,34,56,789.9876 number")
                .build(),
            new TestCase.Builder()
                .pattern("Format {|3.1416|} number")
                .locale("ar-AR-u-nu-latn")
                .expected("Format 3.1416 number")
                .build(),
            new TestCase.Builder()
                .pattern("Format {|3.1416|} number")
                .locale("ar-AR-u-nu-arab")
                .expected("Format ٣٫١٤١٦ number")
                .build(),
    };

    @Test
    public void test() {
        MFParser.debug = false;
        for (TestCase testCase : TEST_CASES) {
            checkOneString(testCase);
        }
    }

    void checkOneString(TestCase testCase) {
        MessageFormatter mf = MessageFormatter.builder()
                .setLocale(testCase.locale)
                .setPattern(testCase.message)
                .build();
        String result = mf.formatToString(testCase.arguments);
        if (!testCase.expected.isEmpty())
            assertEquals(testCase.expected, result);
        else
            System.out.println("GOT : '" + result + "'");
    }
}
