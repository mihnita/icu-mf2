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
            queue.add(new TokenString(Token.Type.EOF, input.buffer, startPos, input.getPosition(), null));
            return new TokenString(Token.Type.PATTERN, input.buffer, startPos, input.getPosition(), "");
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
                    result = new TokenString(Token.Type.RDBLCURLY,
                            input.buffer, startPos, startPos + 2, "}}");
                } else {
                    input.backup(1);
                    result = new TokenString(Token.Type.RCURLY,
                            input.buffer, startPos, startPos + 1, "}");
                }
            } else if (cp == '{') {
                cp = input.readCodePoint();
                if (cp == '{') {
                    result = new TokenString(Token.Type.LDBLCURLY,
                            input.buffer, startPos, startPos + 2, "{{");
                } else {
                    input.backup(1);
                    result = new TokenString(Token.Type.LCURLY,
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
                        result = new TokenString(Token.Type.INPUT, input.buffer, startPos, input.getPosition(), keyworkName);
                        break;
                    case "local":
                        result = new TokenString(Token.Type.LOCAL, input.buffer, startPos, input.getPosition(), keyworkName);
                        break;
                    case "match":
                        result = new TokenString(Token.Type.MATCH, input.buffer, startPos, input.getPosition(), keyworkName);
                        break;
                    default:
                        if (!keyworkName.isEmpty()) {
                            result = new TokenString(Token.Type.RESERVED_KEYWORD, input.buffer, startPos, input.getPosition(), keyworkName);
                        } else {
                            // back to simple message?
                            input.backup(input.getPosition() - startPos);
                            complexMessage = false;
                        }
                        break;
                }
            } else if (isTextChar(cp)) {
                StringBuilder patternBuffer = new StringBuilder();
                patternBuffer.appendCodePoint(cp);
                while (!input.atEnd()) {
                    cp = input.readCodePoint();
                    if (isBackslash(cp)) {
                        cp = input.readCodePoint();
                        // abnf: text-escape = backslash ( backslash / "{" / "}" )
                        if (isBackslash(cp) || cp == '{' || cp == '}')
                            patternBuffer.appendCodePoint(cp);
                        else {
                            error("invalid escape");
                        }
                    } else if (isTextChar(cp)) {
                        patternBuffer.appendCodePoint(cp);
                    }
                }
                if (!input.atEnd()) {
                    input.backup(1);
                }
                result = new TokenString(Token.Type.PATTERN, input.buffer, startPos, input.getPosition(), patternBuffer.toString());
            } else {
                error("Should never get here?");
            }
        }
        if (result == null) {
            return new TokenString(Token.Type.EOF, input.buffer, startPos, input.getPosition(), null);
        }
        return result;
    }

    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?(0|([1-9]\\d*))(\\.\\d*)?([eE][-+]?\\d+)?");
    private Token<?> getNumberLiteral() {
        int start = input.getPosition();
        String tmpBuffer = input.buffer.substring(start);
        Matcher m = NUMBER_PATTERN.matcher(tmpBuffer);
        if (m.matches()) {
            String matchingPart = m.group();
            input.skip(matchingPart.length());
            return new TokenNumber(Token.Type.NUMBER,
                    input.buffer, start, input.getPosition(),
                    Double.valueOf(matchingPart));			
        }
        return new TokenNumber(Token.Type.NUMBER, input.buffer, start, input.getPosition(), 0);
    }

    /**
     * abnf: content-char = %x0000-0008        ; omit HTAB (%x09) and LF (%x0A)
     * abnf:              / %x000B-000C        ; omit CR (%x0D)
     * abnf:              / %x000E-0019        ; omit SP (%x20)
     * abnf:              / %x0021-002D        ; omit . (%x2E)
     * abnf:              / %x002F-003F        ; omit @ (%x40)
     * abnf:              / %x0041-005B        ; omit \ (%x5C)
     * abnf:              / %x005D-007A        ; omit { | } (%x7B-7D)
     * abnf:              / %x007E-D7FF        ; omit surrogates
     * abnf:              / %xE000-10FFFF
     */
    private boolean isContentChar(int c) {
        return c != '\t'
                && c != '\r'
                && c != '\n'
                && c != ' '
                && c != '@'
                && c != '\\'
                && c != '{'
                && c != '|'
                && c != '}'
                //				&& !Character.isSurrogate(c)
                ;
    }

    /*
     * abnf: text-char = content-char / s / "." / "@" / "|"
     */
    private boolean isTextChar(int c) {
        return isContentChar(c) || isWhitespace(c) || c == '.' || c == '@' || c == '|';
    }

    /**
     * abnf: backslash = %x5C ; U+005C REVERSE SOLIDUS "\"
     */
    private boolean isBackslash(int c) {
        return c == '\\';
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
        if (isNameChar(cp)) {
            result.appendCodePoint(cp);
        }
        while (!input.atEnd()){
            cp = input.readCodePoint();
            if (isNameChar(cp) && !input.atEnd()) {
                result.appendCodePoint(cp);
            }
        } 
        return result.toString();
    }

    /**
     * ; Whitespace
     * abnf: s = 1*( SP / HTAB / CR / LF / %x3000 )
     */
    private boolean isWhitespace(int c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\u3000';
    }

    /**
     * abnf: name-start = ALPHA / "_"
     * abnf:            / %xC0-D6 / %xD8-F6 / %xF8-2FF
     * abnf:            / %x370-37D / %x37F-1FFF / %x200C-200D
     * abnf:            / %x2070-218F / %x2C00-2FEF / %x3001-D7FF
     * abnf:            / %xF900-FDCF / %xFDF0-FFFC / %x10000-EFFFF
     */
    private boolean isNameStart(int codePoint) {
        // ALPHA means plain ASCII, A-Z and a-z, see
        // https://en.wikipedia.org/wiki/Augmented_Backus%E2%80%93Naur_form
        return (codePoint >= 'A' && codePoint <= 'Z') // ALPHA
                || (codePoint >= 'a' && codePoint <= 'z') // ALPHA
                || (codePoint >= 0x00C0 && codePoint <= 0x00D6)
                || (codePoint >= 0x00D8 && codePoint <= 0x00F6)
                || (codePoint >= 0x00F8 && codePoint <= 0x02FF)
                || (codePoint >= 0x0370 && codePoint <= 0x037D)
                || (codePoint >= 0x037F && codePoint <= 0x1FFF)
                || (codePoint >= 0x200C && codePoint <= 0x200D)
                || (codePoint >= 0x2070 && codePoint <= 0x218F)
                || (codePoint >= 0x2C00 && codePoint <= 0x2FEF)
                || (codePoint >= 0x3001 && codePoint <= 0xD7FF)
                || (codePoint >= 0xF900 && codePoint <= 0xFDCF)
                || (codePoint >= 0xFDF0 && codePoint <= 0xFFFC)
                || (codePoint >= 0x10000 && codePoint <= 0xEFFFF);
    }

    /**
     * abnf: name-char = name-start / DIGIT / "-" / "."
     * abnf:           / %xB7 / %x300-36F / %x203F-2040
     */
    private boolean isNameChar(int codePoint) {
        // DIGIT means plain ASCII, 0-9, see
        // https://en.wikipedia.org/wiki/Augmented_Backus%E2%80%93Naur_form
        return isNameStart(codePoint)
                || (codePoint >= '0' && codePoint <= '9') // DIGIT
                || codePoint == '-'
                || codePoint == '.'
                || codePoint == 0x00B7
                || (codePoint >= 0x0300 && codePoint <= 0x036F)
                || (codePoint >= 0x203F && codePoint <= 0x2040);
    }

    /*
     * abnf: private-start = "^" / "&"
     */
    private boolean isPrivateStart(int codePoint) {
        return codePoint == '^' || codePoint == '&';
    }

    /*
     * abnf: simple-start = simple-start-char / text-escape / placeholder
     */
    private int getSimpleStart() {
        int cp = input.readCodePoint();
        // abnf: simple-start-char = content-char / s / "@" / "|"
        if (isContentChar(cp) || isWhitespace(cp) || cp == '@' || cp == '|') {
            return cp;
        }
        getTextEscape();
        // placeholder
        return ' ';
    }

    // abnf: text-escape = backslash ( backslash / "{" / "}" )
    private int getTextEscape() {
        int cp = input.readCodePoint();
        if (isBackslash(cp)) {
            cp = input.readCodePoint();
            if (isBackslash(cp) || cp == '{' || cp == '}') {
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
            if (isQuotedChar(cp)) {
                value.appendCodePoint(cp);
            } else if (isBackslash(cp)) {
                cp = input.readCodePoint();
                // abnf: quoted-escape = backslash ( backslash / "|" )
                if (cp == '|' || isBackslash(cp)) {
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
        return new TokenString(Token.Type.STRING,
                input.buffer, start, input.getPosition(), value.toString());
    }

    /*
     * abnf: quoted-char = content-char / s / "." / "@" / "{" / "}"
     */
    private boolean isQuotedChar(int c) {
        return isContentChar(c)
                || isWhitespace(c)
                || c == '.'
                || c == '@'
                || c== '{'
                || c == '}';
    }

    /*
     * abnf: reserved-char = content-char / "."
     */
    private boolean isReservedChar(char c) {
        return isContentChar(c) || c == '.';
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
