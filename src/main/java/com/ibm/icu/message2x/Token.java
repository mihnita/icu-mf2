package com.ibm.icu.message2x;

class Token<T> {
    private static final boolean DEBUG = true;

    public final Kind kind;
    public final String buffer;
    public final int begin;
    public final int end;
    public final T value;
    //	private Token next;
    //	private Token specialToken;
    static enum Kind {
        EOF,
        PATTERN_PART,
        STRING, // |....|
        NUMBER, // abnf: number-literal = ["-"] (%x30 / (%x31-39 *DIGIT)) ["." 1*DIGIT] [%i"e" ["-" / "+"] 1*DIGIT]
        LCURLY, RCURLY, // "{" and "}"
        LDBLCURLY, RDBLCURLY, // "{{" and "}}"
        PATTERN,
        INPUT, // .input
        LOCAL, // .local
        MATCH, // .match
        RESERVED_KEYWORD,
        EQUAL, // "="
        STAR, // "*"
        SLASH, // "/"
        POUND, // "#"
        COLON, // ":"
        AT, // "@"
        DOLLAR, // "$"
        PIPE, // "|"
        MINUS,
        PLUS,
        DOT,
        EXP,
        RESERVED_ANNOTATION,
        RESERVED_START,
        WHITESPACE,
        NAME_START,
        NAME_CHAR,
        CONTENT_CHAR,
        ERROR;
    }

    /**
     * @param kind token type
     * @param begin starting index in string
     * @param end ending index in string
     * @param buffer the original string buffer
     * @param value the real object represented by the input.
     *        For example a real string, with all escapes resolved,
     *        or a number, when the input string was "3.14" or "-3e7"  
     */
    public Token(Kind kind, String buffer, int begin, int end, T value) {
        this.kind = kind;
        this.buffer = buffer;
        this.begin = begin;
        this.end = end;
        this.value = value;
        if (DEBUG) {
            System.out.println(this.toString());
        }
    }

    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("Token [kind=%s, begin=%s, end=%s, image=\"%s\", value=%s]",
                kind, begin, end, buffer.substring(begin, end),
                value instanceof CharSequence ? "\"" + value + "\"" : value);
    }
}
