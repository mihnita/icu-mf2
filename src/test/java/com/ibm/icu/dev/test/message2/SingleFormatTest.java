// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import com.ibm.icu.message2x.MessageFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SuppressWarnings({"static-method", "javadoc"})
public class SingleFormatTest {
    private static final Map<String, Object> ARGS = new HashMap<>();

    static {
        ARGS.put("count", 1);
        ARGS.put("place", 7);
        ARGS.put("fileCount", 1);
        ARGS.put("folderCount", 1);
    }

    @Test
    public void test() {
        String[] testStrings = {
            ".match {$count :number}\n"
                    + "one {{You deleted {$count} file}}\n"
                    + "*   {{You deleted {$count} files}}",
            ".match {$count :number}\n"
                    + "one {{You deleted {$count} file}}"
                    + "*   {{You deleted {$count} files}}",
            ".match {$place :number select=ordinal}\n"
                    + "*   {{You fininshed in the {$place}th place}}\n"
                    + "two {{You fininshed in the {$place}nd place}}\n"
                    + "one {{You fininshed in the {$place}st place}}\n"
                    + "1   {{You got the gold medal}}\n"
                    + "2   {{You got the silver medal}}\n"
                    + "3   {{You got the bronze medal}}\n"
                    + "few {{You fininshed in the {$place}rd place}}\n",
            ".match {$fileCount :number} {$folderCount :number}\n"
                    + "*   *   {{You deleted {$fileCount} files in {$folderCount} folders}}\n"
                    + "one one {{You deleted {$fileCount} file in {$folderCount} folder}}\n"
                    + "one *   {{You deleted {$fileCount} file in {$folderCount} folders}}\n"
                    + "*   one {{You deleted {$fileCount} files in {$folderCount} folder}}\n",
            "{$count :number minimumFractionDigits=2} dollars",
            "{$count :number minimumFractionDigits=3} dollars",
            "{|3.1415| :number minimumFractionDigits=5} dollars",
            "{|3.1415| :number maximumFractionDigits=2} dollars",
            ".local $c = {$count :number minimumFractionDigits=2}\n"
                    + ".match {$c}\n"
                    + "one {{{$c} dollar}}\n"
                    + "*   {{{$c} dollars}}",
            ".local $c = {1 :number minimumFractionDigits=2}\n"
                    + ".match {$c}\n"
                    + "one {{{$c} dollar}}\n"
                    + "*   {{{$c} dollars}}",
            ".local $c = {1 :number}\n"
                    + ".match {$c}\n"
                    + "one {{{$c} dollar}}\n"
                    + "*   {{{$c} dollars}}",
            ".local $c = {1.25 :number}\n"
                    + ".match {$c}\n"
                    + "one {{{$c} dollar}}\n"
                    + "*   {{{$c} dollars}}",
            ".local $c = {1.25 :number maximumFractionDigits=0}\n"
                    + ".match {$c}\n"
                    + "one {{{$c} dollar}}\n"
                    + "*   {{{$c} dollars}}",
            ".match {$count :number} 1 {{one}} * {{other}}",
        };
        for (String test : testStrings) {
            checkOneString(test);
        }
    }

    void checkOneString(String pattern) {
        MessageFormatter mf = MessageFormatter.builder()
                .setLocale(Locale.US)
                .setPattern(pattern)
                .build();
        String result = mf.formatToString(ARGS);
        // TODO!
    }
}
