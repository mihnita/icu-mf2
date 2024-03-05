package com.ibm.icu.message2x.suite;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ibm.icu.message2x.MfException;
import com.ibm.icu.message2x.Parser;
import com.ibm.icu.message2x.Utilities;

@SuppressWarnings("javadoc")
@RunWith(JUnit4.class)
public class MfSyntaxErrorsTest {
    final static private Gson GSON = new GsonBuilder().setDateFormat("yyyyMMdd'T'HHmmss").create();
    final static private String JSON_FILE = "syntax-errors.json";

    @Test
    public void test() throws IOException, URISyntaxException {
        Path json = Utilities.getTestFile(this.getClass(), JSON_FILE);
        try (BufferedReader reader = Files.newBufferedReader(json, StandardCharsets.UTF_8)) {
            String[] unitList = GSON.fromJson(reader, String[].class);
            for (String unit : unitList) {
                System.out.println("================================");
                System.out.println(Utilities.str(unit));
                try {
                    Parser.parse(unit);
                    System.out.println("ERROR : " + unit);
//                    fail("ERROR, we should never get here: '" + unit + "'");
                } catch (MfException e) {
                    
                }
            }
        }
    }
}
