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
    static boolean isContentChar(int c) {
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
    static boolean isWhitespace(int c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\u3000';
    }

    /**
     * abnf: name-start = ALPHA / "_"
     * abnf:            / %xC0-D6 / %xD8-F6 / %xF8-2FF
     * abnf:            / %x370-37D / %x37F-1FFF / %x200C-200D
     * abnf:            / %x2070-218F / %x2C00-2FEF / %x3001-D7FF
     * abnf:            / %xF900-FDCF / %xFDF0-FFFC / %x10000-EFFFF
     */
    static boolean isNameStart(int codePoint) {
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
    static boolean isNameChar(int codePoint) {
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
    static boolean isPrivateStart(int codePoint) {
        return codePoint == '^' || codePoint == '&';
    }

    /*
     * abnf: quoted-char = content-char / s / "." / "@" / "{" / "}"
     */
    static boolean isQuotedChar(int c) {
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
    static boolean isReservedChar(char cp) {
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
}
