package com.ibm.icu.message2x;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SuppressWarnings({ "static-method", "javadoc" })
public class WipTest2 {

    static final boolean IGNORE_OK = false;

    @Rule
    public ErrorCollector collector = new ErrorCollector();

//    @Test(expected = IllegalArgumentException.class)
//    public void testNullInput() {
//        Parser.parse(null);
//    }

    @Test
    public void test() {
        String [] someTests = {
                // Simple messages
                "Hello",
                "Hello world!",
                "Hello \\t \\n \\r \\{ world!",
                "Hello world {:datetime}",
//                "Hello world {$exp}",
//                "Hello world {$exp :datetime}",
//                "Hello world {|2024-02-27| :datetime}",
//                // Simple messages, with declarations
//                ".input foo = {$expiration}",
//                ".input foo = {$expiration :date}",
//                ".local bar = {$expiration :date}",
        };
        for (String test : someTests) {
            System.out.println("======================");
            System.out.println(Utilities.str(test));
            MfDataModel.Message z = Parser.parse(test);
            System.out.println("======================");
            System.out.println(z);
        }
    }
}
