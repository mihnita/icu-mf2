// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import static org.junit.Assert.fail;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ibm.icu.message2x.MFParseException;
import com.ibm.icu.message2x.MFParser;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SuppressWarnings("javadoc")
@RunWith(JUnit4.class)
public class MFDataModelErrorsTest {
    private static final Gson GSON = new GsonBuilder().setDateFormat("yyyyMMdd'T'HHmmss").create();
    private static final String JSON_FILE = "data-model-errors.json";

    @Test
    public void test() throws Exception {
        Path json = Utilities.getTestFile(this.getClass(), JSON_FILE);
        List<String> errors = new ArrayList<>();
        int totalTests = 0;
        try (BufferedReader reader = Files.newBufferedReader(json, StandardCharsets.UTF_8)) {
            String[] unitList = GSON.fromJson(reader, String[].class);
            totalTests = unitList.length;
            for (String unit : unitList) {
                if (unit.startsWith("#")) {
                    continue;
                }
                System.out.println("================================");
                System.out.println(Utilities.str(unit));
                try {
                    MFParser.parse(unit);
                    errors.add(unit);
                } catch (MFParseException e) {
                    // We expected an error, so it's all good
                }
            }
        }

        if (!errors.isEmpty()) {
            MFParser.debug = false;
            System.out.println("===== FAILURES =====");
            for (String error : errors) {
                System.out.println("FAILURE: " + error);
                MFParser.parse(error);
            }
            fail("Undetected errors: " + errors.size() + " / " + totalTests);
        }
    }
}
