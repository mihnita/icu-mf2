package com.ibm.icu.message2x;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.message2x.Token.Kind;

public class RegExpTokenizer {
    private final InputSource input;

    private final Queue<Token<?>> queue = new ArrayDeque<>();

    private final static Pattern RE_LCURLY = Pattern.compile("^\\{");  
    private final static Pattern RE_RCURLY = Pattern.compile("^\\}");
    private final static Pattern RE_LDBLCURLY = Pattern.compile("^\\{\\{");  
    private final static Pattern RE_RDBLCURLY = Pattern.compile("^\\}\\}");
    private final static Pattern RE_EQUAL = Pattern.compile("^=");
    private final static Pattern RE_STAR = Pattern.compile("^\\*");
    
    private final static Pattern RE_SLASH = Pattern.compile("^/");
    private final static Pattern RE_POUND = Pattern.compile("^#");
    private final static Pattern RE_COLON = Pattern.compile("^:");
    private final static Pattern RE_AT = Pattern.compile("^@");
    private final static Pattern RE_DOLLAR = Pattern.compile("^\\$");
    private final static Pattern RE_PIPE = Pattern.compile("^\\|");
//    
    private final static Pattern RE_MINUS = Pattern.compile("^-");
    private final static Pattern RE_PLUS = Pattern.compile("^\\+");
    private final static Pattern RE_DOT = Pattern.compile("^\\.");
    private final static Pattern RE_EXP = Pattern.compile("^[eE]");

    private final static Pattern RE_INPUT = Pattern.compile("^\\.input");
    private final static Pattern RE_LOCAL = Pattern.compile("^\\.local");
    private final static Pattern RE_MATCH = Pattern.compile("^\\.match");

    // abnf: reserved-annotation-start = "!" / "%" / "*" / "+" / "<" / ">" / "?" / "~"
    private final static Pattern RE_RESERVED_ANNOTATION = Pattern.compile("^[!%*+<>?~]");
    // abnf: private-start = "^" / "&"
    private final static Pattern RE_RESERVED_START = Pattern.compile("^[\\^&]");

    // abnf: ; Whitespace
    // abnf: s = 1*( SP / HTAB / CR / LF / %x3000 )
    private final static Pattern RE_WHITESPACE = Pattern.compile("^[ \\t\\n\\r\\x{3000}]+");

    // abnf: name-start = ALPHA / "_"
    // abnf:            / %xC0-D6 / %xD8-F6 / %xF8-2FF
    // abnf:            / %x370-37D / %x37F-1FFF / %x200C-200D
    // abnf:            / %x2070-218F / %x2C00-2FEF / %x3001-D7FF
    // abnf:            / %xF900-FDCF / %xFDF0-FFFC / %x10000-EFFFF
    private final static Pattern RE_NAME_START = Pattern.compile("^["
            + "A-Za-z"
            + "_"
            + "\\x{00C0}-\\x{00D6}"
            + "\\x{00D8}-\\x{00F6}"
            + "\\x{00F8}-\\x{02FF}"
            + "\\x{0370}-\\x{037D}"
            + "\\x{037F}-\\x{1FFF}"
            + "\\x{200C}-\\x{200D}"
            + "\\x{2070}-\\x{218F}"
            + "\\x{2C00}-\\x{2FEF}"
            + "\\x{3001}-\\x{D7FF}"
            + "\\x{F900}-\\x{FDCF}"
            + "\\x{FDF0}-\\x{FFFC}"
            + "\\x{10000}-\\x{EFFFF}"
            + "]");
    private final static Pattern RE_NAME_CHAR = Pattern.compile("^["
            + "A-Za-z"
            + "_"
            + "0-9" // name-char extra, DIGIT
            + "\\-\\." // name-char extra
            + "\\x{00B7}" // name-char extra
            + "\\x{00C0}-\\x{00D6}"
            + "\\x{00D8}-\\x{00F6}"
            + "\\x{00F8}-\\x{02FF}"
            + "\\x{0300}-\\x{036F}" // name-char extra
            + "\\x{0370}-\\x{037D}"
            + "\\x{037F}-\\x{1FFF}"
            + "\\x{200C}-\\x{200D}"
            + "\\x{203F}-\\x{2040}" // name-char extra
            + "\\x{2070}-\\x{218F}"
            + "\\x{2C00}-\\x{2FEF}"
            + "\\x{3001}-\\x{D7FF}"
            + "\\x{F900}-\\x{FDCF}"
            + "\\x{FDF0}-\\x{FFFC}"
            + "\\x{10000}-\\x{EFFFF}"
            + "]+");

    private final static Pattern RE_CATCH_ALL = Pattern.compile(".+");

    // abnf: content-char      = %x00-08        ; omit HTAB (%x09) and LF (%x0A)
    // abnf:                   / %x0B-0C        ; omit CR (%x0D)
    // abnf:                   / %x0E-19        ; omit SP (%x20)
    // abnf:                   / %x21-2D        ; omit . (%x2E)
    // abnf:                   / %x2F-3F        ; omit @ (%x40)
    // abnf:                   / %x41-5B        ; omit \ (%x5C)
    // abnf:                   / %x5D-7A        ; omit { | } (%x7B-7D)
    // abnf:                   / %x7E-D7FF      ; omit surrogates
    // abnf:                   / %xE000-10FFFF
    private final static Pattern RE_CONTENT_CHAR = Pattern.compile("^[^\\t\\r\\n \\.@\\{|}]");

    // The order matters, hence LinkedHashMap
    private final static Map<Token.Kind, Pattern> ALL_TOKENS = new LinkedHashMap<>();
    static {
        // Should put the longest tokens first, so that (for example) `{{` matches before `{`
        ALL_TOKENS.put(Token.Kind.LDBLCURLY, RE_LDBLCURLY);
        ALL_TOKENS.put(Token.Kind.RDBLCURLY, RE_RDBLCURLY);
        ALL_TOKENS.put(Token.Kind.LCURLY, RE_LCURLY);
        ALL_TOKENS.put(Token.Kind.RCURLY, RE_RCURLY);
        ALL_TOKENS.put(Token.Kind.EQUAL, RE_EQUAL);
        ALL_TOKENS.put(Token.Kind.STAR, RE_STAR);
        ALL_TOKENS.put(Token.Kind.SLASH, RE_SLASH);
        ALL_TOKENS.put(Token.Kind.POUND, RE_POUND);
        ALL_TOKENS.put(Token.Kind.COLON, RE_COLON);
        ALL_TOKENS.put(Token.Kind.AT, RE_AT);
        ALL_TOKENS.put(Token.Kind.DOLLAR, RE_DOLLAR);
        ALL_TOKENS.put(Token.Kind.PIPE, RE_PIPE);
        ALL_TOKENS.put(Token.Kind.MINUS, RE_MINUS);
        ALL_TOKENS.put(Token.Kind.PLUS, RE_PLUS);
        ALL_TOKENS.put(Token.Kind.DOT, RE_DOT);
        ALL_TOKENS.put(Token.Kind.EXP, RE_EXP);
        ALL_TOKENS.put(Token.Kind.INPUT, RE_INPUT);
        ALL_TOKENS.put(Token.Kind.LOCAL, RE_LOCAL);
        ALL_TOKENS.put(Token.Kind.MATCH, RE_MATCH);
        ALL_TOKENS.put(Token.Kind.RESERVED_ANNOTATION, RE_RESERVED_ANNOTATION);
        ALL_TOKENS.put(Token.Kind.RESERVED_START, RE_RESERVED_START);
        ALL_TOKENS.put(Token.Kind.WHITESPACE, RE_WHITESPACE);
        ALL_TOKENS.put(Token.Kind.NAME_START, RE_NAME_START);
        ALL_TOKENS.put(Token.Kind.NAME_CHAR, RE_NAME_CHAR);
        ALL_TOKENS.put(Token.Kind.CONTENT_CHAR, RE_CONTENT_CHAR);
//        ALL_TOKENS.put(Token.Kind.ERROR, RE_CATCH_ALL);
    }

    public RegExpTokenizer(InputSource input) throws MfException {
        if (input == null) {
            throw new MfException("Null input");
        }
        this.input = input;
//        if (input.atEnd()) {
//            queue.add(new TokenString(Token.Kind.PATTERN, this.input.buffer, 0, 0, ""));
//        }
    }

    static class StringView implements CharSequence {
        final int offset;
        final String text;

        StringView(String text, int offset) {
            this.offset = offset;
            this.text = text;
        }

        StringView(String text) {
            this(text, 0);
        }

        @Override
        public int length() {
            return text.length() - offset;
        }

        @Override
        public char charAt(int index) {
            return text.charAt(index + offset);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return text.subSequence(start + offset, end + offset);
        }
        
        @Override
        public String toString() {
            return text.substring(offset);
        }
    }

    public Token<?> nextToken() {
        if (queue.isEmpty()) {
            parseNextToken();
        }
        if (!queue.isEmpty()) {
            return queue.poll();
        }
        return new TokenString(Kind.EOF, input.buffer, input.getPosition(), input.getPosition(), null);
    }

    public Token<?> peakNextToken() {
        if (!queue.isEmpty()) {
            return queue.peek();
        }
        parseNextToken();
        if (!queue.isEmpty()) {
            return queue.poll();
        }
        return new TokenString(Kind.EOF, input.buffer, input.getPosition(), input.getPosition(), null);
    }

    private void parseNextToken() {
        int start = input.getPosition();
        Token<?> result = null;
        while (!input.atEnd()) {
            StringView sv = new StringView(input.buffer, input.getPosition());
            for (Entry<Kind, Pattern> e : ALL_TOKENS.entrySet()) {
                result = tryToken(sv, e.getKey(), e.getValue());
                if (result != null) {
                    break;
                }
            }
            if (result == null) {
                result = new TokenString(Kind.ERROR, input.buffer, start, input.getPosition(), null);
            }
            queue.add(result);
            return;
        }
        queue.add(new TokenString(Kind.EOF, input.buffer, start, input.getPosition(), null));
    }

    public void skipToken(Token<?> token) {
        input.gotoPosition(token.end);
    }

    private Token<?> tryToken(StringView sv, Kind kind, Pattern pattern) {
        Matcher m = pattern.matcher(sv);
        boolean found = m.find();
        if (found) {
            int start = input.getPosition();
            int end = start + m.group().length();
            return new TokenString(kind, m.group(), start, input.getPosition(), m.group());
        }
        return null;
    }
    
    public Token<?> tryToken(Kind patternPart) {
        Pattern regExp = ALL_TOKENS.get(patternPart);
        if (regExp != null) {
            StringView sv = new StringView(input.buffer, input.getPosition());
            return tryToken(sv, patternPart, regExp);
        }
        return null;
    }
}
