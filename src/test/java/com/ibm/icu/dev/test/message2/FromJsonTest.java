// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

// import com.ibm.icu.dev.test.CoreTestFmwk;

/**
 * These tests come from the test suite created for the JavaScript implementation of MessageFormat v2.
 *
 * <p>Original JSON file
 * <a href="https://github.com/messageformat/messageformat/blob/master/packages/mf2-messageformat/src/__fixtures/test-messages.json">here</a>.</p>
 */
@RunWith(JUnit4.class)
@SuppressWarnings({"static-method", "javadoc"})
public class FromJsonTest { // extends CoreTestFmwk {

    static final TestCase[] TEST_CASES = {
        new TestCase.Builder().pattern("hello").expected("hello").build(),
        new TestCase.Builder().pattern("hello {|world|}").expected("hello world").build(),
        new TestCase.Builder().pattern("hello {||}").expected("hello ").build(),
        new TestCase.Builder()
                .pattern("hello {$place}")
                .arguments(Args.of("place", "world"))
                .expected("hello world")
                .build(),
        new TestCase.Builder()
                .ignore("TODO: fallback changed?")
                .pattern("hello {$place}")
                .expected("hello {$place}")
                // errorsJs: ["missing-var"]
                .build(),
        new TestCase.Builder()
                .pattern("{$one} and {$two}")
                .arguments(Args.of("one", 1.3, "two", 4.2))
                .expected("1.3 and 4.2")
                .build(),
        new TestCase.Builder()
                .pattern("{$one} et {$two}")
                .locale("fr")
                .arguments(Args.of("one", 1.3, "two", 4.2))
                .expected("1,3 et 4,2")
                .build(),
        new TestCase.Builder().pattern("hello {|4.2| :number}").expected("hello 4.2").build(),
        new TestCase.Builder() // not in the original JSON
                .locale("ar-EG")
                .pattern("hello {|4.2| :number}")
                .expected("hello \u0664\u066B\u0662")
                .build(),
        new TestCase.Builder().pattern("hello {|foo| :number}").expected("hello {|foo|}").build(),
        new TestCase.Builder()
                .pattern("{hello {:number}}")
                .expected("hello {|foo|}")
                // This is different from JS, should be an error.
                .errors("ICU4J: exception.")
                .build(),
        new TestCase.Builder()
                .pattern("hello {|4.2| :number minimumFractionDigits=2}")
                .expected("hello 4.20")
                .build(),
        new TestCase.Builder()
                .pattern("hello {|4.2| :number minimumFractionDigits=|2|}")
                .expected("hello 4.20")
                .build(),
        new TestCase.Builder()
                .pattern("hello {|4.2| :number minimumFractionDigits=$foo}")
                .arguments(Args.of("foo", 2f))
                .expected("hello 4.20")
                .build(),
        new TestCase.Builder()
                .pattern("hello {|4.2| :number minimumFractionDigits=$foo}")
                .arguments(Args.of("foo", "2"))
                .expected("hello 4.20")
                // errorsJs: ["invalid-type"]
                .build(),
        new TestCase.Builder()
                .pattern(".local $foo = {|bar|} {{bar {$foo}}}")
                .expected("bar bar")
                .build(),
        new TestCase.Builder()
                .pattern(".local $foo = {bar} {{bar {$foo}}}")
                .arguments(Args.of("foo", "foo"))
                // expectedJs: "bar foo"
                // It is undefined if we allow arguments to override local variables, or it is an
                // error.
                // And undefined who wins if that happens, the local variable of the argument.
                .expected("bar bar")
                .build(),
        new TestCase.Builder()
                .pattern(".local $foo = {$bar} {{bar {$foo}}}")
                .arguments(Args.of("bar", "foo"))
                .expected("bar foo")
                .build(),
        new TestCase.Builder()
                .pattern(".local $foo = {$bar :number} {{bar {$foo}}}")
                .arguments(Args.of("bar", 4.2))
                .expected("bar 4.2")
                .build(),
        new TestCase.Builder()
                .pattern(".local $foo = {$bar :number minimumFractionDigits=2} {{bar {$foo}}}")
                .arguments(Args.of("bar", 4.2))
                .expected("bar 4.20")
                .build(),
        new TestCase.Builder()
                .ignore("Maybe") // Because minimumFractionDigits=foo
                .pattern(".local $foo = {$bar :number minimumFractionDigits=foo} {{bar {$foo}}}")
                .arguments(Args.of("bar", 4.2))
                .expected("bar 4.2")
                .errors("invalid-type")
                .build(),
        new TestCase.Builder()
                .pattern(".local $foo = {$bar :number} {{bar {$foo}}}")
                .arguments(Args.of("bar", "foo"))
                .expected("bar {|foo|}")
                .build(),
        new TestCase.Builder()
                .pattern(".local $bar = {$baz} .local $foo = {$bar} {{bar {$foo}}}")
                .arguments(Args.of("baz", "foo"))
                .expected("bar foo")
                .build(),
        new TestCase.Builder()
                .patternJs(".match {$foo} 1 {{one}} * {{other}}")
                .pattern(".match {$foo :string} 1 {{one}} * {{other}}")
                .arguments(Args.of("foo", "1"))
                .expected("one")
                .build(),
        new TestCase.Builder()
                .pattern(".match {$foo :number} 1 {{one}} * {{other}}")
                .arguments(Args.of("foo", "1")) // Should this be error? Plural on string?
                // expectedJs: "one"
                .expected("other")
                .build(),
        new TestCase.Builder()
                .pattern(".match {$foo :string} 1 {{one}} * {{other}}")
                .arguments(Args.of("foo", "1"))
                .expected("one")
                .build(),
        new TestCase.Builder()
                .patternJs(".match {$foo} 1 {{one}} * {{other}}")
                .pattern(".match {$foo :number} 1 {{one}} * {{other}}")
                .arguments(Args.of("foo", 1))
                .expected("one")
                .build(),
        new TestCase.Builder()
                .pattern(".match {$foo :number} 1 {{one}} * {{other}}")
                .arguments(Args.of("foo", 1))
                .expected("one")
                .build(),
        new TestCase.Builder()
                .ignore("not possible to put a null in a map")
                .pattern(".match {$foo} 1 {{one}} * {{other}}")
                .arguments(Args.of("foo", null))
                .expected("other")
                .build(),
        new TestCase.Builder()
                .patternJs(".match {$foo} 1 {{one}} * {{other}}")
                .pattern(".match {$foo :number} 1 {{one}} * {{other}}")
                .expected("other")
                .errors("missing-var")
                .build(),
        new TestCase.Builder()
                .patternJs(".match {$foo} one {{one}} * {{other}}")
                .pattern(".match {$foo :number} one {{one}} * {{other}}")
                .arguments(Args.of("foo", 1))
                .expected("one")
                .build(),
        new TestCase.Builder()
                .patternJs(".match {$foo} 1 {{=1}} one {{one}} * {{other}}")
                .pattern(".match {$foo :number} 1 {{=1}} one {{one}} * {{other}}")
                .arguments(Args.of("foo", 1))
                .expected("=1")
                .build(),
        new TestCase.Builder()
                .patternJs(".match {$foo} one {{one}} 1 {{=1}} * {{other}}")
                .pattern(".match {$foo :number} one {{one}} 1 {{=1}} * {{other}}")
                .arguments(Args.of("foo", 1))
                .expected("=1")
                .build(),
        new TestCase.Builder()
                .patternJs(
                        ".match {$foo} {$bar} one one {{one one}} one * {{one other}} * * {{other}}")
                .pattern(
                        ".match {$foo :number} {$bar :number} one one {{one one}} one * {{one other}} * * {{other}}")
                .arguments(Args.of("foo", 1, "bar", 1))
                .expected("one one")
                .build(),
        new TestCase.Builder()
                .patternJs(
                        ".match {$foo} {$bar} one one {{one one}} one * {{one other}} * * {{other}}")
                .pattern(
                        ".match {$foo :number} {$bar :number} one one {{one one}} one * {{one other}} * * {{other}}")
                .arguments(Args.of("foo", 1, "bar", 2))
                .expected("one other")
                .build(),
        new TestCase.Builder()
                .patternJs(
                        ".match {$foo} {$bar} one one {{one one}} one * {{one other}} * * {{other}}")
                .pattern(
                        ".match {$foo :number} {$bar :number} one one {{one one}} one * {{one other}} * * {{other}}")
                .arguments(Args.of("foo", 2, "bar", 2))
                .expected("other")
                .build(),
        new TestCase.Builder()
                .patternJs(".local $foo = {$bar} .match {$foo} one {{one}} * {{other}}")
                .pattern(".local $foo = {$bar} .match {$foo :number} one {{one}} * {{other}}")
                .arguments(Args.of("bar", 1))
                .expected("one")
                .build(),
        new TestCase.Builder()
                .patternJs(".local $foo = {$bar} .match {$foo} one {{one}} * {{other}}")
                .pattern(".local $foo = {$bar} .match {$foo :number} one {{one}} * {{other}}")
                .arguments(Args.of("bar", 2))
                .expected("other")
                .build(),
        new TestCase.Builder()
                .patternJs(".local $bar = {$none} .match {$foo} one {{one}} * {{{$bar}}}")
                .pattern(".local $bar = {$none} .match {$foo :number} one {{one}} * {{{$bar}}}")
                .arguments(Args.of("foo", 1, "none", ""))
                .expected("one")
                .build(),
        new TestCase.Builder()
                .patternJs(".local $bar = {$none} .match {$foo} one {{one}} * {{{$bar}}}")
                .pattern(".local $bar = {$none :number} .match {$foo} one {{one}} * {{{$bar}}}")
                .arguments(Args.of("foo", 2))
                .expected("{$bar}")
                .errors("missing-var")
                .build(),
        new TestCase.Builder()
                .pattern(".local bar = {(foo)} {{$bar}}")
                .expected("{$bar}")
                .errors("missing-char", "missing-var")
                .build(),
        new TestCase.Builder()
                .pattern(".local $bar {(foo)} {{$bar}}")
                .expected("foo")
                .errors("missing-char")
                .build(),
        new TestCase.Builder()
                .pattern(".local $bar = (foo) {{$bar}}")
                .expected("{$bar}")
                .errors("missing-char", "junk-element")
                .build(),
        new TestCase.Builder().pattern("{{#tag}}").expected("#tag").build(),
        new TestCase.Builder().pattern("{#tag}").expected("").build(),
        new TestCase.Builder().pattern("{#tag}content").expected("content").build(),
        new TestCase.Builder().pattern("{#tag}content{/tag}").expected("content").build(),
        new TestCase.Builder().pattern("{#tag}content").expected("content").build(),
        new TestCase.Builder()
                // When we format markup to string we generate no output
                .pattern("{#tag foo=bar}")
                .expected("")
                .build(),
        new TestCase.Builder()
                .pattern("{#tag foo=foo bar=$bar}")
                .arguments(Args.of("bar", "b a r"))
                .expected("")
                .build(),
        new TestCase.Builder()
                .pattern("bad {#markup/} test")
                .expected("bad  test")
                //                .errors("extra-content")
                .build(),
        new TestCase.Builder()
                .pattern("{#tag foo=bar}")
                .expected("")
                //                .errors("extra-content")
                .build(),
        new TestCase.Builder()
                .pattern("no braces")
                .expected("no braces")
                //                .errors("parse-error", "junk-element")
                .build(),
        new TestCase.Builder()
                .pattern("no braces {$foo}")
                .arguments(Args.of("foo", 2))
                .expected("no braces 2")
                //                .errors("parse-error", "junk-element")
                .build(),
        new TestCase.Builder()
                .pattern("{missing end brace")
                .expected("missing end brace")
                .errors("missing-char")
                .build(),
        new TestCase.Builder()
                .pattern("{missing end {$brace")
                .expected("missing end {$brace}")
                .errors("missing-char", "missing-char", "missing-var")
                .build(),
        new TestCase.Builder()
                .pattern("{{extra}} content")
                .expected("extra")
                .errors("extra-content")
                .build(),
        new TestCase.Builder()
                .pattern("empty { }")
                .expected("empty ")
                .errors("parse-error", "junk-element")
                .build(),
        new TestCase.Builder()
                .pattern("bad {:}")
                .expected("bad {:}")
                .errors("empty-token", "missing-func")
                .build(),
        new TestCase.Builder()
                .pattern("bad {placeholder}")
                .expected("bad placeholder")
                //                .errors("parse-error", "extra-content", "junk-element")
                .build(),
        new TestCase.Builder()
                .pattern("no-equal {|42| :number minimumFractionDigits 2}")
                .expected("no-equal 42.00")
                .errors("missing-char")
                .build(),
        new TestCase.Builder()
                .pattern("bad {:placeholder option=}")
                .expected("bad {:placeholder}")
                .errors("empty-token", "missing-func")
                .build(),
        new TestCase.Builder()
                .pattern("bad {:placeholder option value}")
                .expected("bad {:placeholder}")
                .errors("missing-char", "missing-func")
                .build(),
        new TestCase.Builder()
                .pattern("{bad {:placeholder option}}")
                .expected("bad {:placeholder}")
                .errors("missing-char", "empty-token", "missing-func")
                .build(),
        new TestCase.Builder()
                .pattern("{bad {$placeholder option}}")
                .expected("bad {$placeholder}")
                .errors("extra-content", "extra-content", "missing-var")
                .build(),
        new TestCase.Builder()
                .pattern("{no {$placeholder end}")
                .expected("no {$placeholder}")
                .errors("extra-content", "missing-var")
                .build(),
        new TestCase.Builder()
                .pattern(".match {} * {foo}")
                .expected("foo")
                .errors("parse-error", "bad-selector", "junk-element")
                .build(),
        new TestCase.Builder()
                .pattern(".match {+foo} * {foo}")
                .expected("foo")
                .errors("bad-selector")
                .build(),
        new TestCase.Builder()
                .pattern(".match {(foo)} when*{foo}")
                .expected("foo")
                .errors("missing-char")
                .build(),
        new TestCase.Builder()
                .pattern(".match * {foo}")
                .expected("foo")
                .errors("empty-token")
                .build(),
        new TestCase.Builder()
                .pattern(".match {(x)} * foo")
                .expected("")
                .errors("key-mismatch", "missing-char")
                .build(),
        new TestCase.Builder()
                .pattern(".match {(x)} * {foo} extra")
                .expected("foo")
                .errors("extra-content")
                .build(),
        new TestCase.Builder()
                .pattern(".match (x) * {foo}")
                .expected("")
                .errors("empty-token", "extra-content")
                .build(),
        new TestCase.Builder()
                .pattern(".match {$foo} * * {foo}")
                .expected("foo")
                .errors("key-mismatch", "missing-var")
                .build(),
        new TestCase.Builder()
                .pattern(".match {$foo} {$bar} * {foo}")
                .expected("foo")
                .errors("key-mismatch", "missing-var", "missing-var")
                .build()
    };

    @Test
    public void test() {
        int ignoreCount = 0;
        for (TestCase testCase : TEST_CASES) {
            if (testCase.ignore) {
                ignoreCount++;
            }
            TestUtils.runTestCase(testCase);
        }
        System.out.printf(
                "Executed %d test cases out of %d, skipped %d%n",
                TEST_CASES.length - ignoreCount, TEST_CASES.length, ignoreCount);
    }
}
