package com.ibm.icu.message2x;

class TokenNumber extends Token<Number> {
    public TokenNumber(Kind kind, String buffer, int begin, int end, Number value) {
        super(kind, buffer, begin, end, value);
    }
}
