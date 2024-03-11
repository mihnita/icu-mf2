// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.message2x.MFParser;
import com.ibm.icu.message2x.MessageFormatter;

@RunWith(JUnit4.class)
@SuppressWarnings({ "static-method", "javadoc" })
public class SingleFormatTest {
    static private final Map<String, Object> ARGS = new HashMap<>();
    static {
        ARGS.put("count", 21);
        ARGS.put("place", 4);
        ARGS.put("fileCount", 12);
        ARGS.put("folderCount", 5);
    }

    @Test
    public void test() {
        String[] testStrings = {
//                ".match {$count :number}\n"
//                        + "one {{You deleted {$count} file}}\n"
//                        + "*   {{You deleted {$count} files}}",
                ".match {$count :number}\n"
                        + "one {{You deleted {$count} file}}"
                        + "*   {{You deleted {$count} files}}",
//                ".match {$place :number select=ordinal}\n"
//                        + "*   {{You fininshed in the {$place}th place}}\n"
//                        + "two {{You fininshed in the {$place}nd place}}\n"
//                        + "one {{You fininshed in the {$place}st place}}\n"
//                        + "1   {{You got the gold medal}}\n"
//                        + "2   {{You got the silver medal}}\n" + "3 {{You got the bronze medal}}\n"
//                        + "few {{You fininshed in the {$place}rd place}}\n"
//                        ,
                ".match {$fileCount :number} {$folderCount :number}\n"
                        + "one one {{You deleted {$fileCount} file in {$folderCount} folder}}\n"
                        + "one *   {{You deleted {$fileCount} file in {$folderCount} folders}}\n"
                        + "*   one {{You deleted {$fileCount} files in {$folderCount} folder}}\n"
                        + "*   *   {{You deleted {$fileCount} files in {$folderCount} folders}}\n",
//                "{$count :number minimumFractionDigits=2} dollars",
//                "{$count :number minimumFractionDigits=3} dollars",
//                "{|3.1415| :number minimumFractionDigits=5} dollars",
//                "{|3.1415| :number maximumFractionDigits=2} dollars",
//                ""
//                        + ".local $c = {$count :number minimumFractionDigits=2}\n"
//                        + ".match {$c}\n"
//                        + "one {{{$c} dollar}}\n"
//                        + "*   {{{$c} dollars}}",
        };
        for (String test : testStrings) {
            MFParser.debug = false;
            checkOneString(test);
        }
    }

    void checkOneString(String pattern) {
        System.out.println("========================");
        System.out.println(Utilities.str(pattern));

        MessageFormatter mf = MessageFormatter.builder().setLocale(Locale.US).setPattern(pattern).build();
        String result = mf.formatToString(ARGS);
        System.out.println("RESULT: " + result);
    }
}
