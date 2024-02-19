package com.ibm.icu.message2x.suite;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Unit test for simple App.
 */
@RunWith(JUnit4.class)
public class Mf2SyntaxErrorsTest {
    Gson gson = new GsonBuilder().setDateFormat("yyyyMMdd'T'HHmmss").create();

    /**
     * Rigorous Test :-)
     * @throws IOException 
     */
    @Test
    public void test() throws IOException {
        try (FileReader reader = new FileReader("")) {
            List<String> tests = gson.fromJson(reader, ArrayList.class);
            for (String test : tests) {
                System.out.println(test);
            }
        }
    }
}
