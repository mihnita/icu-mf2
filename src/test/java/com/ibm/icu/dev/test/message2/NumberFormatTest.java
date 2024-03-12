// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import static org.junit.Assert.assertEquals;

import com.ibm.icu.message2x.MFParser;
import com.ibm.icu.message2x.MessageFormatter;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SuppressWarnings({"static-method", "javadoc"})
public class NumberFormatTest {
    private static final Map<String, Object> ARGS = new HashMap<>();

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
            new TestCase.Builder()
                    .pattern("Format {3.1415926 :number}")
                    .expected("Format 3.141593")
                    .build(),
            new TestCase.Builder()
                    .pattern("Format {3.1415926 :number maximumFractionDigits=4}")
                    .expected("Format 3.1416")
                    .build(),
            new TestCase.Builder()
                    .pattern("Format {3 :number minimumFractionDigits=2}")
                    .expected("Format 3.00")
                    .build(),
            new TestCase.Builder()
                    .pattern("Format {3.2 :number minimumFractionDigits=2}")
                    .expected("Format 3.20")
                    .build(),
            new TestCase.Builder()
                    .pattern("Format {123456789.97531 :number maximumSignificantDigits=4}")
                    .expected("Format 123,500,000")
                    .build(),
            new TestCase.Builder()
                    .pattern("Format {3.1415926 :number}")
                    .expected("Format 3.141593")
                    .build(),
            new TestCase.Builder()
                    .pattern("Numbering system {123456 :number numberingSystem=deva}")
                    .expected("Numbering system १२३,४५६")
                    .build(),
            new TestCase.Builder()
                    .pattern("Percent {0.1416 :number style=percent}")
                    .expected("Percent 14.16%")
                    .build(),
            new TestCase.Builder()
                    .pattern("Scientific {123456789.97531 :number notation=scientific}")
                    .expected("Scientific 1.234568E8")
                    .build(),
            new TestCase.Builder()
                    .pattern("Engineering {123456789.97531 :number notation=engineering}")
                    .expected("Engineering 123.45679E6")
                    .build(),
            new TestCase.Builder()
                    .pattern("Compact {123456789.97531 :number notation=compact}")
                    .expected("Compact 123M")
                    .build(),
            new TestCase.Builder()
                    .pattern("Compact {123456789.97531 :number notation=compact compactDisplay=long}")
                    .expected("Compact 123 million")
                    .build(),
            new TestCase.Builder()
                    .pattern("Compact {123456789.97531 :number notation=compact compactDisplay=short}")
                    .expected("Compact 123M")
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
        if (!testCase.expected.isEmpty()) {
            assertEquals(testCase.expected, result);
        } else {
            System.out.println("GOT : '" + result + "'");
        }
    }
}
