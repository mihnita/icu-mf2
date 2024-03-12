// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import static org.junit.Assert.fail;

import com.ibm.icu.message2x.MessageFormatter;
import java.io.Reader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SuppressWarnings({ "static-method", "javadoc" })
@RunWith(JUnit4.class)
public class MFSyntaxErrorsTest {
    private static final String JSON_FILE = "syntax-errors.json";

    @Test
    public void test() throws Exception {
        try (Reader reader = TestUtils.jsonReader(JSON_FILE)) {
            String[] unitList = TestUtils.GSON.fromJson(reader, String[].class);
            for (String pattern : unitList) {
                try {
                    MessageFormatter.builder().setPattern(pattern).build().formatToString(null);
                    fail("Undetected errors in: '" + pattern + "'");
                } catch (Exception e) {
                    // We expected an error, so it's all good
                }
            }
        }
    }
}
