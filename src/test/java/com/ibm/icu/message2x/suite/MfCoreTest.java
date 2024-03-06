package com.ibm.icu.message2x.suite;

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
import com.ibm.icu.message2x.MfDataModel;
import com.ibm.icu.message2x.MfParser;
import com.ibm.icu.message2x.Utilities;

@SuppressWarnings({ "javadoc" })
@RunWith(JUnit4.class)
public class MfCoreTest {
    final static private Gson GSON = new GsonBuilder().setDateFormat("yyyyMMdd'T'HHmmss").create();
    final static private String JSON_FILE = "test-core.json";

    @Test
    public void test() throws IOException, URISyntaxException {
        Path json = Utilities.getTestFile(this.getClass(), JSON_FILE);
        try (BufferedReader reader = Files.newBufferedReader(json, StandardCharsets.UTF_8)) {
            Unit[] unitList = GSON.fromJson(reader, Unit[].class);
            for (Unit unit : unitList) {
                System.out.println("================================");
                System.out.println(unit.src);
                MfDataModel.Message message = MfParser.parse(unit.src);
                System.out.println(message);
            }
        }
    }
}
