// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.message2x;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Ignore;

//import com.ibm.icu.message2x.MessageFormatter;
//import com.ibm.icu.message2x.MFFunctionRegistry;

@Ignore("Utility class, has no test methods.")
/** Utility class, has no test methods. */
public class TestUtils {

    static void runTestCase(TestCase testCase) {
        runTestCase(null, testCase);
    }

    static void runTestCase(MFFunctionRegistry customFunctionsRegistry, TestCase testCase) {
        if (testCase.ignore) {
            return;
        }

        // We can call the "complete" constructor with null values, but we want to test that
        // all constructors work properly.
        MessageFormatter.Builder mfBuilder = MessageFormatter.builder()
                .setPattern(testCase.message)
                .setLocale(testCase.locale);
        if (customFunctionsRegistry != null) {
            mfBuilder.setFunctionRegistry(customFunctionsRegistry);
        }
        try { // TODO: expected error
            MessageFormatter mf = mfBuilder.build();
            String result = mf.formatToString(testCase.arguments);
            if (!testCase.errors.isEmpty()) {
                fail(reportCase(testCase) + "\nExpected error, but it didn't happen.\n"
                        + "Result: '" + result + "'");
            } else {
                assertEquals(reportCase(testCase), testCase.expected, result);
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            if (testCase.errors.isEmpty()) {
                fail(reportCase(testCase) + "\nNo error was expected here, but it happened:\n"
                        + e.getMessage());
            }
        }
    }

    private static String reportCase(TestCase testCase) {
        return testCase.toString();
    }
}
