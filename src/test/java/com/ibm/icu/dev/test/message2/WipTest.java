// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import com.ibm.icu.message2x.MFDataModel;
import com.ibm.icu.message2x.MFParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SuppressWarnings({"static-method", "javadoc"})
public class WipTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNullInput() throws Exception {
        MFParser.parse(null);
    }

    @Test
    public void test() throws Exception {
        String[] someTests = {
            // Simple messages
            "",
            "Hello",
            "Hello world!",
            "Hello \\t \\n \\r \\{ world!",
            "Hello world {:datetime}",
            "Hello world {foo}",
            "Hello {0} world",
            "Hello {123} world",
            "Hello {-123} world",
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
            // Attributes
            "Hello world {$exp}",
            "Hello world {$exp @attr}",
            "Hello world {$exp @valid @attr=a @attrb=123 @atrn=|foo bar|}",
            "Hello world {$exp :date @valid @attr=aaaa @attrb=123 @atrn=|foo bar|}",
            "Hello world {$exp :date year=numeric month=long day=numeric int=12 @valid @attr=a @attrb=123 @atrn=|foo bar|}",
            // Reserved
            "Reserved {$exp &foo |something more protected|} and more", // private
            "Reserved {$exp %foo |something quoted \\| inside|} and more", // reserved
            "{{.starting with dot is OK here}}",
            "{{Some string pattern, with {$foo} and {$exp :date style=long}!}}",
            // Simple messages, with declarations
            ".input {$pi :number}",
            ".input {$exp :date}",
            ".local $foo = {$exp}",
            ".local $foo = {$exp :date}",
            ".local $foo = {$exp :date year=numeric month=long day=numeric}",
            ".local $bar = {$foo :date month=medium}",
            ".someting |reserved=| {$foo :date}",
            // Several declarations in one message
            ""
                    + ".input {$a :date}\n"
                    + ".local $b = {$a :date year=numeric month=long day=numeric}\n"
                    + ".local $c = {$b :date month=medium}\n"
                    + ".someting |reserved = \\| and more| {$x :date} {$y :date} {$z :number}",
            ""
                    + ".input {$a :date}\n"
                    + ".local $exp = {$a :date style=full}\n"
                    + "{{Your card expires on {$exp}!}}",
            // Selectors
            ".match {$count :number}\n"
                    + "  1 {{You deleted one file}}\n"
                    + "  * {{You deleted {$count} files}}   ",
        };
        for (String test : someTests) {
            System.out.println("======================");
            System.out.println(Utilities.str(test));
            MFDataModel.Message z = MFParser.parse(test);
            System.out.println("======================");
            System.out.println(z);
        }
    }
}
