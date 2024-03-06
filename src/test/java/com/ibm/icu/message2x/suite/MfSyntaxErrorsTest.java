package com.ibm.icu.message2x.suite;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ibm.icu.message2x.MfException;
import com.ibm.icu.message2x.MfParser;
import com.ibm.icu.message2x.Utilities;

@SuppressWarnings("javadoc")
@RunWith(JUnit4.class)
public class MfSyntaxErrorsTest {
    final static private Gson GSON = new GsonBuilder().setDateFormat("yyyyMMdd'T'HHmmss").create();
    final static private String JSON_FILE = "syntax-errors.json";

    @Test
    public void test() throws Exception {
        Path json = Utilities.getTestFile(this.getClass(), JSON_FILE);
        List<String> errors = new ArrayList<>();
        int totalTests = 0;
        try (BufferedReader reader = Files.newBufferedReader(json, StandardCharsets.UTF_8)) {
            String[] unitList = GSON.fromJson(reader, String[].class);
            totalTests = unitList.length;
            for (String unit : unitList) {
                System.out.println("================================");
                System.out.println(Utilities.str(unit));
                try {
                    MfParser.parse(unit);
                    System.out.println("UNDETECTED (BAD) : " + unit);
                    errors.add(unit);
                } catch (MfException e) {
                    System.out.println("DETECTED (GOOD)  : " + e.getMessage());
                }
            }
        }

        if (!errors.isEmpty()) {
            System.out.println("===== FAILURES =====");
            for (String error : errors) {
                System.out.println("FAILURE: " + error);
                MfParser.parse(error);
            }
            fail("Undetected errors: " + errors.size() + " / " + totalTests);
        }
    }
}
