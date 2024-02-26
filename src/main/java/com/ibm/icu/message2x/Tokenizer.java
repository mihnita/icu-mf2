package com.ibm.icu.message2x;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer {
    private final InputSource input;
    private boolean complexMessage = false;
    private boolean firstTime = true;

    private final Queue<Token<?>> queue = new ArrayDeque<>();

    public Tokenizer(String input) {
        this.input = new InputSource(input);
    }

    public Token<?> nextToken() {
        if (!queue.isEmpty()) {
            return queue.poll();
        }

        int startPos = input.getPosition();
        if (startPos == 0 && input.buffer.isEmpty()) {
            // Empty string
            queue.add(new TokenString(Token.Kind.EOF, input.buffer, startPos, input.getPosition(), null));
            return new TokenString(Token.Kind.PATTERN, input.buffer, startPos, input.getPosition(), "");
        }

        Token<?> result = null;
        while (!input.atEnd()) {
            int cp = input.readCodePoint();
            if (firstTime) {
                firstTime = false;
                if (startPos == 0 && cp == '.') { // complex message
                    // A bit of a cheat, to simplify things.
                    // TODO: check the behavior for ".", ".123" and other strings that are probably simple-message
                    complexMessage = true;
                }
            }
            if (cp == '}') {
                cp = input.readCodePoint();
                if (cp == '}') {
                    result = new TokenString(Token.Kind.RDBLCURLY,
                            input.buffer, startPos, startPos + 2, "}}");
                } else {
                    input.backup(1);
                    result = new TokenString(Token.Kind.RCURLY,
                            input.buffer, startPos, startPos + 1, "}");
                }
            } else if (cp == '{') {
                cp = input.readCodePoint();
                if (cp == '{') {
                    result = new TokenString(Token.Kind.LDBLCURLY,
                            input.buffer, startPos, startPos + 2, "{{");
                } else {
                    result = new TokenString(Token.Kind.LCURLY,
                            input.buffer, startPos, startPos + 1, "{");
                }
            } else if (cp == '|') {
                input.backup(1);
                result = getQuoted();
            } else if (cp == '-' || (cp >= '0' && cp <= '9')) {
                // abnf: number-literal = ["-"] (%x30 / (%x31-39 *DIGIT)) ["." 1*DIGIT] [%i"e" ["-" / "+"] 1*DIGIT]
                input.backup(1);
                result = getNumberLiteral();
            } else if (complexMessage && cp == '.') {
                String keyworkName = getKeyword();
                switch (keyworkName) {
                    case "input":
                        result = new TokenString(Token.Kind.INPUT, input.buffer, startPos, input.getPosition(), keyworkName);
                        break;
                    case "local":
                        result = new TokenString(Token.Kind.LOCAL, input.buffer, startPos, input.getPosition(), keyworkName);
                        break;
                    case "match":
                        result = new TokenString(Token.Kind.MATCH, input.buffer, startPos, input.getPosition(), keyworkName);
                        break;
                    default:
                        if (!keyworkName.isEmpty()) {
                            result = new TokenString(Token.Kind.RESERVED_KEYWORD, input.buffer, startPos, input.getPosition(), keyworkName);
                        } else {
                            // back to simple message?
                            input.backup(input.getPosition() - startPos);
                            complexMessage = false;
                        }
                        break;
                }
            } else if (StringUtils.isTextChar(cp)) {
                StringBuilder patternBuffer = new StringBuilder();
                patternBuffer.appendCodePoint(cp);
                while (!input.atEnd()) {
                    cp = input.readCodePoint();
                    if (StringUtils.isBackslash(cp)) {
                        cp = input.readCodePoint();
                        // abnf: text-escape = backslash ( backslash / "{" / "}" )
                        if (StringUtils.isBackslash(cp) || cp == '{' || cp == '}')
                            patternBuffer.appendCodePoint(cp);
                        else {
                            error("invalid escape");
                        }
                    } else if (StringUtils.isTextChar(cp)) {
                        patternBuffer.appendCodePoint(cp);
                    } else {
                        break;
                    }
                }
                if (!input.atEnd()) {
                    input.backup(1);
                }
                result = new TokenString(Token.Kind.PATTERN, input.buffer, startPos, input.getPosition(), patternBuffer.toString());
            } else {
                error("Should never get here?");
            }
            if (result != null) {
                return result;
            }
        }
        return new TokenString(Token.Kind.EOF, input.buffer, startPos, input.getPosition(), null);
    }

    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?(0|([1-9]\\d*))(\\.\\d*)?([eE][-+]?\\d+)?");
    private Token<?> getNumberLiteral() {
        int start = input.getPosition();
        String tmpBuffer = input.buffer.substring(start);
        Matcher m = NUMBER_PATTERN.matcher(tmpBuffer);
        if (m.matches()) {
            String matchingPart = m.group();
            input.skip(matchingPart.length());
            return new TokenNumber(Token.Kind.NUMBER,
                    input.buffer, start, input.getPosition(),
                    Double.valueOf(matchingPart));			
        }
        return new TokenNumber(Token.Kind.NUMBER, input.buffer, start, input.getPosition(), 0);
    }

    /* Covers all tokens that start with a '.', for now `.input`, `.local`, `.match`, and `reserved-keyword`
     * abnf: ; Keywords; Note that these are case-sensitive
     * abnf: input = %s".input"
     * abnf: local = %s".local"
     * abnf: match = %s".match"
     * abnf: reserved-keyword   = "." name
     */
    private String getKeyword() {
        if (input.atEnd()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        int cp = input.readCodePoint();
        if (StringUtils.isNameChar(cp)) {
            result.appendCodePoint(cp);
        }
        while (!input.atEnd()){
            cp = input.readCodePoint();
            if (StringUtils.isNameChar(cp) && !input.atEnd()) {
                result.appendCodePoint(cp);
            }
        } 
        return result.toString();
    }

    /*
     * abnf: simple-start = simple-start-char / text-escape / placeholder
     */
    private int getSimpleStart() {
        int cp = input.readCodePoint();
        // abnf: simple-start-char = content-char / s / "@" / "|"
        if (StringUtils.isContentChar(cp) || StringUtils.isWhitespace(cp) || cp == '@' || cp == '|') {
            return cp;
        }
        getTextEscape();
        // placeholder
        return ' ';
    }

    // abnf: text-escape = backslash ( backslash / "{" / "}" )
    private int getTextEscape() {
        int cp = input.readCodePoint();
        if (StringUtils.isBackslash(cp)) {
            cp = input.readCodePoint();
            if (StringUtils.isBackslash(cp) || cp == '{' || cp == '}') {
                return cp;
            } else {
                error("Invalid escape sequence, only '\\', '{' and '}' are acceptable here.");
            }
        }
        return cp;
    }

    /*
     * abnf: quoted = "|" *(quoted-char / quoted-escape) "|"
     */
    private Token<String> getQuoted() {
        StringBuilder value = new StringBuilder();
        int start = input.getPosition();
        int cp = input.readCodePoint();
        if (cp != '|') {
            error("Expecter starting '|' at offset {}, found {}");
        }
        do {
            cp = input.readCodePoint();
            if (StringUtils.isQuotedChar(cp)) {
                value.appendCodePoint(cp);
            } else if (StringUtils.isBackslash(cp)) {
                cp = input.readCodePoint();
                // abnf: quoted-escape = backslash ( backslash / "|" )
                if (cp == '|' || StringUtils.isBackslash(cp)) {
                    value.appendCodePoint(cp);
                } else {
                    error("Invalid escape sequence \\{c}");
                }
            } else if (cp == '|') {
                // end of string. Exit the loop, and don't include it in the parsed value
                break;
            } else {
                value.appendCodePoint(cp);
            }
        } while (!input.atEnd());
        if (cp != '|') {
            error("Expecter terminating '|' at offset {}, found {}");
        }
        return new TokenString(Token.Kind.STRING,
                input.buffer, start, input.getPosition(), value.toString());
    }

    /* Other literals to encode:
     * "{{"
     * "}}"
     * "="
     * "*"
     * "{"
     * "}"
     * "/"
     * "#"
     * ":"
     * "@"
     * "$"
     * "|"
     * "."
     * reserved-annotation-start
     * "!" | "%" | "*" | "+" | "<" | ">" | "?" | "~"
     *
     * "-" => for number-literal
     * "." => for number-literal
     * "e" => for number-literal
     * "-" => for number-literal
     * "+" => for number-literal
     *
     * ".input"
     * ".local"
     * ".match" 
     */
    private void error(String string) {
        // TODO Auto-generated method stub
    }
}
