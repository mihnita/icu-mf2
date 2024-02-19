package com.ibm.icu.message2x;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hamcrest.core.IsEqual;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.message2x.Token.Type;

@RunWith(JUnit4.class)
@SuppressWarnings({ "static-method", "javadoc" })
public class WipTest {
  
  static final boolean IGNORE_OK = true;
	
  @Rule
  public ErrorCollector collector = new ErrorCollector();

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
      return new MiniToken<>(other.getKind(), other.getValue());
    }
    
    @Override
    public int hashCode() {
      return Objects.hash(kind, value);
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
    final List<MiniToken<?>> expectedTokens;

    static enum Test {
      SKIP, // These are failing, but skip them for now
      WIP, // Work in progress. Failing, but working on them right now 
      OK; // Known to be working, but skip to avoid triggering breakpoints
    }

    TokenizerTestCase(String input, Test skip, List<MiniToken<?>> expectedTokens) {
      this.skip = skip;
      this.input = input;
      this.expectedTokens = expectedTokens;
    }
    TokenizerTestCase(String input, List<MiniToken<?>> expectedTokens) {
      this(input, Test.OK, expectedTokens);
    }
  }

  private static final TokenizerTestCase [] TOK_TEST = {
      new TokenizerTestCase("", // TokenizerTestCase.Test.SKIP,
          Arrays.asList(
              new MiniToken<>(Token.Type.PATTERN, ""),
              new MiniToken<>(Token.Type.EOF, null)
              )),
      new TokenizerTestCase(".",
          Arrays.asList(
              new MiniToken<>(Token.Type.PATTERN, "."),
              new MiniToken<>(Token.Type.EOF, null)
              )),
      new TokenizerTestCase("Hello world",
          Arrays.asList(
              new MiniToken<>(Token.Type.PATTERN, "Hello world"),
              new MiniToken<>(Token.Type.EOF, null)
              )),
      new TokenizerTestCase("{{Hello world}}", TokenizerTestCase.Test.SKIP,
          Arrays.asList(
              new MiniToken<>(Token.Type.LDBLCURLY, "{{"),
              new MiniToken<>(Token.Type.PATTERN, "Hello world"),
              new MiniToken<>(Token.Type.RDBLCURLY, "}}"), // missing
              new MiniToken<>(Token.Type.EOF, null)
              )),
      new TokenizerTestCase("|Hello world|",
          Arrays.asList(
              new MiniToken<>(Token.Type.STRING, "Hello world"),
              new MiniToken<>(Token.Type.EOF, null)
              )),
      new TokenizerTestCase("|Hello\tworld|",
          Arrays.asList( // real tab, does not get to the parser 
              new MiniToken<>(Token.Type.STRING, "Hello\tworld"),
              new MiniToken<>(Token.Type.EOF, null)
              )),
      new TokenizerTestCase("|Hello\tworld|",
          Arrays.asList( // escaped tab, gets to the parser
              new MiniToken<>(Token.Type.STRING, "Hello\tworld"),
              new MiniToken<>(Token.Type.EOF, null)
              )),
      new TokenizerTestCase("|Hello|world|", TokenizerTestCase.Test.SKIP,
          Arrays.asList(
              new MiniToken<>(Token.Type.STRING, "Hello"),
              new MiniToken<>(Token.Type.EOF, null)
          )),
      new TokenizerTestCase("|Hello\\|world|",
          Arrays.asList(
              new MiniToken<>(Token.Type.STRING, "Hello|world"),
              new MiniToken<>(Token.Type.EOF, null)
          )),
      new TokenizerTestCase("|Hello\\\\|world|", TokenizerTestCase.Test.SKIP,
          // Java makes this into Backslash-Backslash-Pipe, and that is what MF2 sees.
          // MF2 unescapes Backslash-Backslash to Backslash and puts it in the "raw string".
          // And coming after that the `|` marks the end of the string, it is not unescaped.
          Arrays.asList(
              new MiniToken<>(Token.Type.STRING, "Hello\\|world"),
              new MiniToken<>(Token.Type.EOF, null)
          )),
      new TokenizerTestCase("Hello {$user}", TokenizerTestCase.Test.SKIP,
          Arrays.asList(
            new MiniToken<>(Token.Type.PATTERN, "Hello "),
//            new MiniToken<>(Token.Type.EXPRESSION, "{$user}"),
            new MiniToken<>(Token.Type.LCURLY, "{"),
//            new MiniToken<>(Token.Type.EXPRESSION, "{$user}"),
            new MiniToken<>(Token.Type.RCURLY, "}"),
            new MiniToken<>(Token.Type.EOF, null)
          )),  
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
    List<Token<?>> actual;
    List<?> actualMini;
    for (TokenizerTestCase test : TOK_TEST) {
      switch (test.skip) {
        case OK:
          if (IGNORE_OK)
            break;
          actual = Parser.tokenizeAll(test.input);
          actualMini = actual.stream().map(MiniToken::fromToken).collect(Collectors.toList());
          collector.checkThat(actualMini, IsEqual.equalTo(test.expectedTokens));
          break;
        case WIP:
          System.out.println("======================");
          System.out.println("" + test.skip + " !!!");
          System.out.println(Utilities.str(test.input));
          actual = Parser.tokenizeAll(test.input);
          actualMini = actual.stream().map(MiniToken::fromToken).collect(Collectors.toList());
          System.out.println("Expected:");
          System.out.println(actualMini.toString());
          break;
        case SKIP:
          break;
        default:
          System.out.println("" + test.skip + " !!!");
      }
    }
  }
}
