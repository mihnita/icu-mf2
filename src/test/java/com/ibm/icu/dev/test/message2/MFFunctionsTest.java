// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import java.io.BufferedReader;
import java.lang.reflect.Type;
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
import com.ibm.icu.message2x.MFDataModel;
import com.ibm.icu.message2x.MFParser;

@SuppressWarnings({ "javadoc" })
@RunWith(JUnit4.class)
public class MFFunctionsTest {
    final static private Gson GSON = new GsonBuilder().setDateFormat("yyyyMMdd'T'HHmmss").create();
    final static private String JSON_FILE = "test-functions.json";

    @Test
    public void test() throws Exception {
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
                    System.out.println(message);
                }
            }
        }
    }
}
