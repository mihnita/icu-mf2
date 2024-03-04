package com.ibm.icu.message2x.suite;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ibm.icu.message2x.Utilities;

@SuppressWarnings({ "javadoc" })
@RunWith(JUnit4.class)
public class MfFunctionsTest {
    final static private String JSON_FILE = "test-functions.json";
    Gson gson = new GsonBuilder().create();

    @Test
    public void test() throws IOException, URISyntaxException {
        Path json = Utilities.getTestFile(this.getClass(), JSON_FILE);
        try (BufferedReader reader = Files.newBufferedReader(json, StandardCharsets.UTF_8)) {
            Type mapType = new TypeToken<Map<String, Unit[]>>(){}.getType();
            Map<String, Unit[]> unitList = gson.fromJson(reader, mapType);
            for (Entry<String, Unit[]> testGroup : unitList.entrySet()) {
                System.out.println(testGroup.getKey());
                for (Unit unit : testGroup.getValue()) {
                    System.out.println("    " + unit);
                }
            }
        }
    }
}
