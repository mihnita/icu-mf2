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
                "Hello world {foo}",
                "Hello {0} world",
                "Hello {123} world",
                "Hello {-123} world",
//                "Hello {0123} world", // fails, as expected
//                "Hello {-0123} world", // fails, as expected
                "Hello {3.1416} world",
                "Hello {-3.1416} world",
                "Hello {123E+2} world",
                "Hello {123E-2} world",
                "Hello {123.456E+2} world",
                "Hello {123.456E-2} world",
                "Hello {-123.456E+2} world",
                "Hello {-123.456E-2} world",
                "Hello {-123E+2} world",
                "Hello {-123E-2} world",
                //===
                "Hello world {$exp}",
                "Hello world {$exp :datetime}",
                "Hello world {|2024-02-27| :datetime}",
                "Hello world {$exp :datetime style=long} and more",
                "Hello world {$exp :function number=1234} and more",
                "Hello world {$exp :function unquoted=left   } and more",
                "Hello world {$exp :function quoted=|Something| } and more",
                "Hello world {$exp :function quoted=|Something with spaces| } and more",
                "Hello world {$exp :function quoted=|Something with \\| spaces and \\| escapes| } and more",
                "Hello world {$exp :function number=1234 unquoted=left quoted=|Something|}",
                "Hello world {$exp :function number=1234 unquoted=left quoted=|Something longer|}",
                "Hello world {$exp :function number=1234 unquoted=left quoted=|Something \\| longer|}",
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
