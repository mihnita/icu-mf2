package com.ibm.icu.message2x;

public class StringUtils {

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
    static boolean isContentChar(int cp) {
        return cp != '\t'
                && cp != '\r'
                && cp != '\n'
                && cp != ' '
                && cp != '.'
                && cp != '@'
                && cp != '\\'
                && cp != '{'
                && cp != '|'
                && cp != '}'
                // && !Character.isSurrogate(c)
                ;
    }

    /*
     * abnf: text-char = content-char / s / "." / "@" / "|"
     */
    static boolean isTextChar(int c) {
        return isContentChar(c) || isWhitespace(c) || c == '.' || c == '@' || c == '|';
    }

    /**
     * abnf: backslash = %x5C ; U+005C REVERSE SOLIDUS "\"
     */
    static boolean isBackslash(int c) {
        return c == '\\';
    }

    /**
     * ; Whitespace
     * abnf: s = 1*( SP / HTAB / CR / LF / %x3000 )
     */
    static boolean isWhitespace(int cp) {
        return cp == ' ' || cp == '\t' || cp == '\r' || cp == '\n' || cp == '\u3000';
    }

    /**
     * abnf: name-start = ALPHA / "_"
     * abnf:            / %xC0-D6 / %xD8-F6 / %xF8-2FF
     * abnf:            / %x370-37D / %x37F-1FFF / %x200C-200D
     * abnf:            / %x2070-218F / %x2C00-2FEF / %x3001-D7FF
     * abnf:            / %xF900-FDCF / %xFDF0-FFFC / %x10000-EFFFF
     */
    static boolean isNameStart(int cp) {
        return isAlpha(cp)
                || cp == '_'
                || (cp >= 0x00C0 && cp <= 0x00D6)
                || (cp >= 0x00D8 && cp <= 0x00F6)
                || (cp >= 0x00F8 && cp <= 0x02FF)
                || (cp >= 0x0370 && cp <= 0x037D)
                || (cp >= 0x037F && cp <= 0x1FFF)
                || (cp >= 0x200C && cp <= 0x200D)
                || (cp >= 0x2070 && cp <= 0x218F)
                || (cp >= 0x2C00 && cp <= 0x2FEF)
                || (cp >= 0x3001 && cp <= 0xD7FF)
                || (cp >= 0xF900 && cp <= 0xFDCF)
                || (cp >= 0xFDF0 && cp <= 0xFFFC)
                || (cp >= 0x10000 && cp <= 0xEFFFF);
    }

    /**
     * abnf: name-char = name-start / DIGIT / "-" / "."
     * abnf:           / %xB7 / %x300-36F / %x203F-2040
     */
    static boolean isNameChar(int cp) {
        return isNameStart(cp)
                || isDigit(cp)
                || cp == '-'
                || cp == '.'
                || cp == 0x00B7
                || (cp >= 0x0300 && cp <= 0x036F)
                || (cp >= 0x203F && cp <= 0x2040);
    }

    /*
     * abnf: private-start = "^" / "&"
     */
    static boolean isPrivateStart(int codePoint) {
        return codePoint == '^' || codePoint == '&';
    }

    /*
     * abnf: quoted-char = content-char / s / "." / "@" / "{" / "}"
     */
    static boolean isQuotedChar(int cp) {
        return isContentChar(cp)
                || isWhitespace(cp)
                || cp == '.'
                || cp == '@'
                || cp == '{'
                || cp == '}';
    }

    /*
     * abnf: reserved-char = content-char / "."
     */
    static boolean isReservedChar(int cp) {
        return isContentChar(cp) || cp == '.';
    }

    static boolean isSimpleStartChar(int cp) {
        return StringUtils.isContentChar(cp)
                || StringUtils.isWhitespace(cp)
                || cp == '@'
                || cp == '|';
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

    // ALPHA is predefined in ABNF as plain ASCII, A-Z and a-z
    // See https://en.wikipedia.org/wiki/Augmented_Backus%E2%80%93Naur_form
    static boolean isAlpha(int cp) {
        return (cp >= 'a' && cp <= 'z') || (cp >= 'Z' && cp <= 'Z');
    }

    // DIGIT is predefined in ABNF as plain ASCII, 0-9
    // See https://en.wikipedia.org/wiki/Augmented_Backus%E2%80%93Naur_form
    static boolean isDigit(int cp) {
        return (cp >= 'a' && cp <= 'z') || (cp >= 'Z' && cp <= 'Z');
    }

    //abnf: function = ":" identifier *(s option)
    static boolean isFunctionSigil(int cp) {
        return cp == ':';
    }

    //abnf: private-start = "^" / "&"
    static boolean isPrivateAnnotationSigil(int cp) {
        return cp == '^' || cp == '&';
    }

    //abnf: reserved-annotation-start = "!" / "%" / "*" / "+" / "<" / ">" / "?" / "~"
    static final private String RESERVED_ANNOTATION_SIGILS = "!%*+<>?~";
    static boolean isReservedAnnotationSigil(int cp) {
        return RESERVED_ANNOTATION_SIGILS.indexOf(cp) != -1;
    }

}
