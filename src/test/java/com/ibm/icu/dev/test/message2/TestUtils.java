// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ibm.icu.message2x.MessageFormatter;
import org.junit.Ignore;

/** Utility class, has no test methods. */
@Ignore("Utility class, has no test methods.")
public class TestUtils {
    static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();


    static boolean expectsErrors(Unit unit) {
        return unit.errors != null && !unit.errors.isEmpty();
    }

    static void runTestCase(Unit unit) {
        runTestCase(unit, null);
    }

    static void runTestCase(Unit unit, Map<String, Object> params) {
        if (unit.ignore != null) {
            return;
        }

        StringBuilder pattern = new StringBuilder();
        if (unit.srcs != null) {
            for (String src : unit.srcs) {
                pattern.append(src);
            }
        } else if (unit.src != null){
            pattern.append(unit.src);
        }

        // We can call the "complete" constructor with null values, but we want to test that
        // all constructors work properly.
        MessageFormatter.Builder mfBuilder = MessageFormatter.builder()
                .setPattern(pattern.toString());
        if (unit.locale != null && !unit.locale.isEmpty()) {
            mfBuilder.setLocale(Locale.forLanguageTag(unit.locale));
        } else {
            mfBuilder.setLocale(Locale.US);
        }

        try {
            MessageFormatter mf = mfBuilder.build();
            if (unit.params != null) {
                params = unit.params;
            }
            String result = mf.formatToString(params);
            if (expectsErrors(unit)) {
                fail(reportCase(unit)
                        + "\nExpected error, but it didn't happen.\n"
                        + "Result: '" + result + "'");
            } else {
                assertEquals(reportCase(unit), unit.exp, result);
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            if (!expectsErrors(unit)) {
                fail(reportCase(unit)
                        + "\nNo error was expected here, but it happened:\n"
                        + e.getMessage());
            }
        }
    }
    
    private static String reportCase(Unit unit) {
        return unit.toString();
    }

    static Reader jsonReader(String jsonFileName) throws URISyntaxException, IOException {
        Path json = Utilities.getTestFile(TestUtils.class, jsonFileName);
        return Files.newBufferedReader(json, StandardCharsets.UTF_8);
    }
}
