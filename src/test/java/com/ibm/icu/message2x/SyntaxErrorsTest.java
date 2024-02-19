package com.ibm.icu.message2x;

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

@RunWith(JUnit4.class)
public class SyntaxErrorsTest {
    final Gson gson = new GsonBuilder().create();
    final static private String JSON_FILE = "syntax-errors.json";

    @Test
    public void test() throws IOException, URISyntaxException {
        Path json = Utilities.getTestFile(this.getClass(), JSON_FILE);
        try (BufferedReader reader = Files.newBufferedReader(json, StandardCharsets.UTF_8)) {
            String[] unitList = gson.fromJson(reader, String[].class); 
            for (String unit : unitList) {
                System.out.println(Utilities.str(unit));
            }
        }
    }
}
