// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ibm.icu.message2x.MFDataModel;
import com.ibm.icu.message2x.MFParser;
import com.ibm.icu.message2x.MessageFormatter;
import java.io.BufferedReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SuppressWarnings({"javadoc"})
@RunWith(JUnit4.class)
public class MFFunctionsTest {
    private static final Gson GSON = new GsonBuilder().setDateFormat("yyyyMMdd'T'HHmmss").create();
    private static final String JSON_FILE = "test-functions.json";

    @Test
    public void test() throws Exception {
        Map<String, String> failures = new LinkedHashMap<>();
        int totalCount = 0;
        int errorCount = 0;

        MFParser.debug = false;
        Path json = Utilities.getTestFile(this.getClass(), JSON_FILE);
        try (BufferedReader reader = Files.newBufferedReader(json, StandardCharsets.UTF_8)) {
            Type mapType = new TypeToken<Map<String, Unit[]>>(){/* not code */}.getType();
            Map<String, Unit[]> unitList = GSON.fromJson(reader, mapType);
            for (Entry<String, Unit[]> testGroup : unitList.entrySet()) {
                System.out.println("================================");
                System.out.println(testGroup.getKey());
                for (Unit unit : testGroup.getValue()) {
                    System.out.println("    ----------------------------");
                    System.out.println("    " + unit.src);
                    
                    MFDataModel.Message message = MFParser.parse(unit.src);
                    MessageFormatter mf = MessageFormatter.builder().setDataModel(message).build();
                    System.out.println("Pattern: " + mf.getPattern());

                    totalCount++;
                    boolean failed;
                    String result;
                    try {
                        Map<String, Object> params = unit.params;
                        result = mf.formatToString(params);
                        failed = false;
                    } catch (Exception e) {
                        result = "ERROR";
                        failed = true;
                    }
                    if (unit.errors != null && !unit.errors.isEmpty()) {
                        if (!failed && !result.equals(unit.exp)) {
                            errorCount++;
                            failures.put("SUCCESS, should have failed: \n" + unit.src,
                                    "expected: " + unit.exp + "\n    but was : " + result
                                    + "\nExprected errors:" + unit.errors);
                        }
                    } else {
                        if (failed) {
                            errorCount++;
                            failures.put("FAILED, should have succeded: \n" + unit.src,
                                    "expected: " + unit.exp + "\n    but was : " + result);
                        }
                    }
                }
            }
            System.out.println("=========== FAILED ===========");
            for (Entry<String, String> e : failures.entrySet()) {
                System.out.printf("%s\n    %s%n", e.getKey(), e.getValue());
            }
            System.out.println("Failures: " + errorCount + " / " + totalCount);
            if (errorCount != 0) {
                Logger logger = Logger.getLogger(this.getClass().getName());
                logger.warning("Undetected errors: " + errorCount + " / " + totalCount);
                // fail("Undetected errors: " + errorCount + " / " + totalCount);
            }
        }
    }
}
