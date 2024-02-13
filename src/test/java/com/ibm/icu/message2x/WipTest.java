package com.ibm.icu.message2x;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.message2x.Token.Type;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class WipTest {

  @Test(expected = IllegalArgumentException.class)
  public void testNullInput() {
    Parser.parse(null);
  }

  static private class MiniToken<T> {
    final Token.Type kind;
    final T value;

    public MiniToken(Type kind, T value) {
      super();
      this.kind = kind;
      this.value = value;
    }
    
    static public MiniToken<?> fromToken(Token<?> other) {
      return new MiniToken<>(other.kind, other.value);
    }
    
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() == obj.getClass()) {
        MiniToken<?> other = (MiniToken<?>) obj;
        return kind == other.kind && Objects.equals(value, other.value);
      }
      if (obj instanceof Token) {
        MiniToken<?> other = fromToken((Token<?>) obj);
        return kind == other.kind && Objects.equals(value, other.value);
      }
      return false;
    }

    @Override
    public String toString() {
      if (value instanceof CharSequence) {
        return String.format("Token [kind=%s, value=\"%s\"]", kind, value);
      } else {
        return String.format("Token [kind=%s, value=%s]", kind, value);
      }
    }
  }

  static private class TokenizerTestCase {
    final Test skip;
    final String input;
    final List<MiniToken> expectedTokens;

    static enum Test {
      SKIP, RUN, TODO;
    }

    TokenizerTestCase(String input, Test skip, List<MiniToken> expectedTokens) {
      this.skip = skip;
      this.input = input;
      this.expectedTokens = expectedTokens;
    }
    TokenizerTestCase(String input, List<MiniToken> expectedTokens) {
      this(input, Test.RUN, expectedTokens);
    }
  }

  private static final TokenizerTestCase [] TOK_TEST = {
//      new TokenizerTestCase("", TokenizerTestCase.Test.SKIP,
//          Arrays.asList(
//              new MiniToken<>(Token.Type.PATTERN, ""),
//              new MiniToken<>(Token.Type.EOF, null)
//              )),
//      new TokenizerTestCase(".",
//          Arrays.asList(
//              new MiniToken<>(Token.Type.PATTERN, "."),
//              new MiniToken<>(Token.Type.EOF, null)
//              )),
//      new TokenizerTestCase("Hello world",
//          Arrays.asList(
//              new MiniToken<>(Token.Type.PATTERN, "Hello world"),
//              new MiniToken<>(Token.Type.EOF, null)
//              )),
//      new TokenizerTestCase("{{Hello world}}", TokenizerTestCase.Test.SKIP,
//          Arrays.asList(
//              new MiniToken<>(Token.Type.LDBLCURLY, "{{"),
//              new MiniToken<>(Token.Type.PATTERN, "Hello world"),
//              new MiniToken<>(Token.Type.RDBLCURLY, "}}"), // missing
//              new MiniToken<>(Token.Type.EOF, null)
//              )),
//      new TokenizerTestCase("|Hello world|",
//          Arrays.asList(
//              new MiniToken<>(Token.Type.STRING, "Hello world"),
//              new MiniToken<>(Token.Type.EOF, null)
//              )),
//      new TokenizerTestCase("|Hello\tworld|",
//          Arrays.asList( // real tab, does not get to the parser 
//              new MiniToken<>(Token.Type.STRING, "Hello\tworld"),
//              new MiniToken<>(Token.Type.EOF, null)
//              )),
//      new TokenizerTestCase("|Hello\tworld|",
//          Arrays.asList( // escaped tab, gets to the parser
//              new MiniToken<>(Token.Type.STRING, "Hello\tworld"),
//              new MiniToken<>(Token.Type.EOF, null)
//              )),
      new TokenizerTestCase("|Hello|world|", TokenizerTestCase.Test.TODO,
          Arrays.asList(
              new MiniToken<>(Token.Type.STRING, "Hello"),
              new MiniToken<>(Token.Type.EOF, null)
          )),
//      new TokenizerTestCase("|Hello\\|world|",
//          Arrays.asList(
//              new MiniToken<>(Token.Type.STRING, "Hello|world"),
//              new MiniToken<>(Token.Type.EOF, null)
//          )),
      new TokenizerTestCase("|Hello\\\\|world|", TokenizerTestCase.Test.TODO,
          // Java makes this into Backslash-Backslash-Pipe, and that is what MF2 sees.
          // MF2 unescapes Backslash-Backslash to Backslash and puts it in the "raw string".
          // And coming after that the `|` marks the end of the string, it is not unescaped.
          Arrays.asList(
              new MiniToken<>(Token.Type.STRING, "Hello\\|world"),
              new MiniToken<>(Token.Type.EOF, null)
          )),
//      new TokenizerTestCase("Hello {$user}", TokenizerTestCase.Test.SKIP,
//          Arrays.asList(
//            new MiniToken<>(Token.Type.PATTERN, "Hello "),
////            new MiniToken<>(Token.Type.EXPRESSION, "Hello "),
//            new MiniToken<>(Token.Type.EOF, null)
//          )),  
  };

  @Test @Ignore
  public void test() {
    String [] someTests = {
        // Numbers
        "-1",
        "0",
        "-0",
        "+0",
        "0.00",
//        "-0.00", // LOOP
        "+0.00",
//        "-.00", // LOOP
        "+.00",
        "1",
        "+1",
        "-1234",
        "1234",
        "+1234",
        "1.00",
        "-1.00",
        "+1.00",
        "3.1416",
        "-3.1416",
        "+3.1416",
        ".1416",
//        "-.1416", // LOOP
        "+.1416",
        "7e12",
        "7e-12",
        "-7e12",
        "-7e-12",
        ".input foo = {$expiration}",
        ".input foo = {$expiration :date}",
    };
    for (String test : someTests) {
      System.out.println("======================");
      System.out.println(Utilities.str(test));
      Parser.parse(test);
    }
  }

  @Test
  public void testTok() {
    for (TokenizerTestCase test : TOK_TEST) {
      System.out.println("======================");
      System.out.println(Utilities.str(test.input));
      if (test.skip == TokenizerTestCase.Test.RUN) {
        List<Token<?>> actual = Parser.tokenizeAll(test.input);
        List<?> actualMini = actual.stream().map(MiniToken::fromToken).collect(Collectors.toList());
        assertEquals(test.expectedTokens.size(), actualMini.size());
        assertEquals(test.expectedTokens, actualMini);
      } else if (test.skip == TokenizerTestCase.Test.TODO) {
        System.out.println("" + test.skip + " !!!");
        List<Token<?>> actual = Parser.tokenizeAll(test.input);
        List<?> actualMini = actual.stream().map(MiniToken::fromToken).collect(Collectors.toList());
        System.out.println("Expected:");
        System.out.println(actualMini.toString());
      } else {
        System.out.println("" + test.skip + " !!!");
      }
    }
  }  

@Test
public void testTokSS() {
  Locale jdkNo = Locale.forLanguageTag("no");
  Locale jdkNb = Locale.forLanguageTag("nb");
  Locale jdkNy = Locale.forLanguageTag("ny");

  System.out.printf("no ? nb : %s%n", jdkNo.equals(jdkNb));
  System.out.printf("no ? ny : %s%n", jdkNo.equals(jdkNy));
  System.out.printf("nb ? no : %s%n", jdkNb.equals(jdkNo));
  System.out.printf("nb ? ny : %s%n", jdkNb.equals(jdkNy));
  System.out.printf("ny ? no : %s%n", jdkNy.equals(jdkNo));
  System.out.printf("ny ? nb : %s%n", jdkNy.equals(jdkNb));
  
  ULocale icuNo = ULocale.forLanguageTag("no");
  ULocale icuNb = ULocale.forLanguageTag("nb");
  ULocale icuNy = ULocale.forLanguageTag("ny");
  
  System.out.printf("no ? nb : %s%n", icuNo.equals(icuNb));
  System.out.printf("no ? ny : %s%n", icuNo.equals(icuNy));
  System.out.printf("nb ? no : %s%n", icuNb.equals(icuNo));
  System.out.printf("nb ? ny : %s%n", icuNb.equals(icuNy));
  System.out.printf("ny ? no : %s%n", icuNy.equals(icuNo));
  System.out.printf("ny ? nb : %s%n", icuNy.equals(icuNb));    
}
}
