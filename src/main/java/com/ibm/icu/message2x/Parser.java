package com.ibm.icu.message2x;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Parser {
    final InputSource input;
    final RegExpTokenizer tokenizer;

    private Parser(String text) {
        this.input = new InputSource(text);
        this.tokenizer = new RegExpTokenizer(input);
    }

    static public Mf2DataModel.Message parse(String input) {
        return new Parser(input).parseImpl();
    }
    // abnf: message           = simple-message / complex-message

    // visible for debugging
    static List<Token<?>> tokenizeAll(String input) {
        Parser parser = new Parser(input);
        List<Token<?>> result = new ArrayList<>();
        Token<?> token;
        do {
            token = parser.tokenizer.nextToken();	
            result.add(token);
        } while (token.kind != Token.Kind.EOF);
        return result;
    }

    // Parser proper
    public Mf2DataModel.Message parseImpl() {
        int cp = input.peakChar();
        if (cp == '.') { // declarations or .match
            ComplexMessage complexMessage = getComplexMessage();
            spy("complexMessage", complexMessage);
        } else if (cp == '{') { // `{` or `{{`
            cp = input.readCodePoint();
            if (cp == '{') { // `{{`, complex body without declarations
                input.backup(1);
                ComplexMessage complexBody = getComplexBody();
                spy("complexBody", complexBody);
            } else { // placeholder
                SimpleMessage simpleMessage = getSimpleMessage();
                spy("simpleMessage1", simpleMessage);
            }
        } else {
            SimpleMessage simpleMessage = getSimpleMessage();
            spy("simpleMessage2", simpleMessage);
        }
        return null;
    }

    static class SimpleMessage {
        final Mf2DataModel.Pattern parts;

        public SimpleMessage(Mf2DataModel.Pattern parts) {
            this.parts = parts;
        }
        @Override
        public String toString() {
            return "SimpleMessage { parts:" + parts.toString() + "}";
        }
    }

    static class ComplexMessage {
    }

    // abnf: simple-message    = [simple-start pattern]
    // abnf: simple-start      = simple-start-char / text-escape / placeholder
    // abnf: pattern           = *(text-char / text-escape / placeholder)
    private SimpleMessage getSimpleMessage() {
        Mf2DataModel.Pattern parts = new Mf2DataModel.Pattern();
        while (true) {
            Mf2DataModel.PatternPart part = getPatternPart();
            if (part == null) {
                break;
            }
            spy("part", part);
            parts.parts.add(part);
        }
        return new SimpleMessage(parts);
    }

    private Mf2DataModel.PatternPart getPatternPart() {
        int cp = input.peakChar();
        switch (cp) {
            case -1: // EOF
                return null;
            case '{':
                Mf2DataModel.Expression ph = getPlaceholder();
                spy("placeholder", ph);
                return ph;
            default:
                String plainText = getText();
                Mf2DataModel.StringPart sp = new Mf2DataModel.StringPart(plainText);
                spy("plainText", plainText);
                return sp;
        }
    }

    private String getText() {
        StringBuilder result = new StringBuilder();
        while (true) {
            int cp = input.readCodePoint();
            switch (cp) {
                case -1: // EOF
                    return result.toString();
                case '\\':
                    cp = input.readCodePoint();
                    if (cp == '\\' || cp == '{'|| cp == '|' | cp == '}') {
                        result.appendCodePoint(cp);
                    } else { // TODO: Error, invalid escape
                        result.appendCodePoint('\\');
                        result.appendCodePoint(cp);
                    }
                    break;
                case '.':
                case '@':
                case '|':
                    result.appendCodePoint(cp);
                    break;
                default:
                    if (isContentChar(cp) || isWhitespace(cp) ) {
                        result.appendCodePoint(cp);
                    } else {
                        input.backup(1);
                        return result.toString();
                    }
            }
        }
    }

    //abnf: ; Whitespace
    //abnf: s = 1*( SP / HTAB / CR / LF / %x3000 )
    static private boolean isWhitespace(int cp) {
        return cp == ' ' || cp == '\t' || cp == '\r' || cp == '\n' || cp == '\u3000';
    }

    //abnf: content-char      = %x00-08        ; omit HTAB (%x09) and LF (%x0A)
    //abnf:                   / %x0B-0C        ; omit CR (%x0D)
    //abnf:                   / %x0E-19        ; omit SP (%x20)
    //abnf:                   / %x21-2D        ; omit . (%x2E)
    //abnf:                   / %x2F-3F        ; omit @ (%x40)
    //abnf:                   / %x41-5B        ; omit \ (%x5C)
    //abnf:                   / %x5D-7A        ; omit { | } (%x7B-7D)
    //abnf:                   / %x7E-D7FF      ; omit surrogates
    //abnf:                   / %xE000-10FFFF
    static private boolean isContentChar(int cp) {
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
                ;
    }

    //abnf: placeholder       = expression / markup
    //abnf: expression            = literal-expression
    //abnf:                       / variable-expression
    //abnf:                       / annotation-expression
    //abnf: literal-expression    = "{" [s] literal [s annotation] *(s attribute) [s] "}"
    //abnf: variable-expression   = "{" [s] variable [s annotation] *(s attribute) [s] "}"
    //abnf: annotation-expression = "{" [s] annotation *(s attribute) [s] "}"
    //abnf: markup = "{" [s] "#" identifier *(s option) *(s attribute) [s] ["/"] "}"  ; open and standalone
    //abnf:        / "{" [s] "/" identifier *(s option) *(s attribute) [s] "}"  ; close
    private Mf2DataModel.Expression getPlaceholder() {
        int cp = input.readCodePoint();
        if (cp != '{') {
            return null;
        }
        Mf2DataModel.Expression result = null;
        skipOptionalWhitespaces();
        cp = input.readCodePoint();
        switch (cp) {
                case '#': // open or standalone markup
                case '/': // close markup
                    Object mk = getMarkup();
                    break;
                case ':': // annotation, function
                    //abnf: function       = ":" identifier *(s option)
                    String identifier = getIdentifier();
                    spy("identifier", identifier);
                    List<Mf2DataModel.Option> options = getOptions();
                    spy("options", options);
                    Mf2DataModel.FunctionAnnotation fa = new Mf2DataModel.FunctionAnnotation(identifier, options);
                    result = new Mf2DataModel.FunctionExpression(fa, null);
                    break;
                case '^': // intentional fallthrough
                case '&': // annotation, private
                    //abnf: private-start = "^" / "&"
                    break;
                case '!': // intentional fallthrough
                case '%': // intentional fallthrough
                case '*': // intentional fallthrough
                case '+': // intentional fallthrough
                case '<': // intentional fallthrough
                case '>': // intentional fallthrough
                case '?': // intentional fallthrough
                case '~': // reserved-annotation
                    //abnf: reserved-annotation-start = "!" / "%" / "*" / "+" / "<" / ">" / "?" / "~"
                    break;
                case '$': // variable
                    //abnf: variable       = "$" name
                    Mf2DataModel.VariableRef var = getVariable();
                    break;
                default: // literal, we hope
                    Mf2DataModel.LiteralOrVariableRef litOrVar = getLiteralOrVariableRef();
        }
        cp = input.readCodePoint();
        if (cp != '}') {
            error("Placeholder not closed");
        }
        return result;
    }

    //abnf: identifier = [namespace ":"] name
    //abnf: namespace  = name
    //abnf: name       = name-start *name-char
    private String getIdentifier() {
        String namespace = getName();
        if (namespace == null) {
            return null;
        }
        int cp = input.readCodePoint();
        if (cp == ':') { // the previous name was namespace
            String name = getName();
            if (name == null) {
                error("Expected name after namespace '" + namespace + "'");
            }
            return namespace + ":" + name;
        } else {
            input.backup(1);
        }
        return namespace;
    }

    //abnf helper: *(s option)
    private List<Mf2DataModel.Option> getOptions() {
        List<Mf2DataModel.Option> options = new ArrayList<>();
        while (true) {
            Mf2DataModel.Option option = getOption();
            if (option == null) {
                break;
            }
            options.add(option);
        }
        return options;
    }

    //abnf: option         = identifier [s] "=" [s] (literal / variable)
    private Mf2DataModel.Option getOption() {
        // TODO Auto-generated method stub
        skipOptionalWhitespaces();
        String identifier = getIdentifier();
        if (identifier == null) {
            return null;
        }
        skipOptionalWhitespaces();
        int cp = input.readCodePoint();
        if (cp != '=') {
            error("Expected '='");
            return null;
        }
        skipOptionalWhitespaces();
        Mf2DataModel.LiteralOrVariableRef litOrVar = getLiteralOrVariableRef();
        return null;
    }

    private Mf2DataModel.VariableRef getVariable() {
        return null;
    }

    //abnf: variable       = "$" name
    //abnf: literal        = quoted / unquoted
    private Mf2DataModel.LiteralOrVariableRef getLiteralOrVariableRef() {
        int cp = input.readCodePoint();
        switch (cp) {
            case '$': // variableRef
                //abnf: variable       = "$" name
                String name = getName();
                spy("varName", name);
                break;
            case '|': // quoted
                //abnf: quoted         = "|" *(quoted-char / quoted-escape) "|"
                input.backup(1);
                Mf2DataModel.Literal ql = getQuotedLiteral();
                spy("QuotedLiteral", ql);
                break;
            default : // unquoted
                Mf2DataModel.Literal unql = getUnQuotedLiteral();
                spy("UnQuotedLiteral", unql);
        }
        return null;
    }

    private Mf2DataModel.Literal getQuotedLiteral() {
        StringBuilder result = new StringBuilder();
        int cp = input.readCodePoint();
        if (cp != '|') {
            error("expected starting '|'");
        }
        while (true) {
                cp = input.readCodePoint();
                if (cp == -1) { // EOF
                    break;
                } else if (isQuotedChar(cp)) {
                    result.appendCodePoint(cp);
                } else if (cp == '\\') {
                    cp = input.readCodePoint();
                    if (cp == '|') {
                        result.appendCodePoint('|');
                    } else {
                        error("Invalid escape sequence, only \"\\|\" is valid here");
                    }
                } else {
                    break;
                }
        }
        if (cp != '|') {
            error("expected ending '|'");
        }
        return null;
    }

    static private boolean isQuotedChar(int cp) {
        return isContentChar(cp)
                | isWhitespace(cp)
                | cp == '.'
                | cp == '@'
                | cp == '{'
                | cp == '}';

    }

    private Mf2DataModel.Literal getUnQuotedLiteral() {
        return null;
    }

    private void skipMandatoryWhitespaces() {
        int count = skipWhitespaces();
        if (count <= 0) {
            error("Space expected");
        }
    }
    private void skipOptionalWhitespaces() {
        skipWhitespaces();
    }

    private Object getMarkup() {
        // TODO Auto-generated method stub
        return null;
    }

    private int skipWhitespaces() {
        int skipCount = 0;
        while(true) {
            int cp = input.readCodePoint();
            if (cp == -1) { // EOF
                return skipCount;
            }
            if (!isWhitespace(cp)) {
                input.backup(1);
                return skipCount;
            }
            skipCount++;
        }
    }

    private class Placeholder {
    }

    private void getPattern() {
    }

    private ComplexMessage getComplexMessage() {
        List<Mf2DataModel.Declaration> declarations = new ArrayList<>();
        while (true) {
            Mf2DataModel.Declaration declaration = getDeclaration();
            if (declaration == null) {
                break;
            }
            declarations.add(declaration);
        }
        return null;
    }

    private Mf2DataModel.Declaration getDeclaration() {
        return null;
    }

    // complex-body      = quoted-pattern / matcher
    // quoted-pattern    = "{{" pattern "}}"
    private ComplexMessage getComplexBody() { // {{ ... }}
        return null;
    }

    // abnf: input = %s".input"
    // abnf: local = %s".local"
    // abnf: match = %s".match"
    // abnf: reserved-keyword   = "." name
    private String getKeyword() {
        int cp = input.readCodePoint();
        if (cp != '.')
            return null;
        return "." + getName();
    }

    private String getName() {
        StringBuilder result = new StringBuilder();
        int cp = input.readCodePoint();
        if (!isNameStart(cp)) {
            input.backup(1);
            return null;
        }
        result.appendCodePoint(cp);
        while (true) {
            cp = input.readCodePoint();
            if (isNameChar(cp)) {
                result.appendCodePoint(cp);
            } else {
                input.backup(1);
                break;
            }
        }
        return result.toString();
    }

    private void error(String message) throws Mf2Exception {
        StringBuilder finalMsg = new StringBuilder();
        if (input == null) {
            finalMsg.append("Parse error: ");
            finalMsg.append(message);
        } else {
            int position = input.getPosition();
            finalMsg.append("Parse error [" + input.getPosition() + "]: ");
            finalMsg.append(message);
            finalMsg.append("\n");
            if (position != -1) {
                finalMsg.append(input.buffer.substring(0, position));
                finalMsg.append("^^^");
                finalMsg.append(input.buffer.substring(position));
            } else {
                finalMsg.append(input.buffer);
            }
        }
        throw new RuntimeException(finalMsg.toString());
    }

    private void spy(String label, Object obj) {
        int position = input.getPosition();
        System.out.printf("SPY: %s: %s%n", label, Objects.toString(obj));
//        System.out.printf("%s: %s // [%d] '%s\u2191%s'%n", label, Objects.toString(obj),
//                position,
//                input.buffer.substring(0, position),
//                input.buffer.substring(position)
//                );
    }

    //abnf: name-start = ALPHA / "_"
    //abnf:            / %xC0-D6 / %xD8-F6 / %xF8-2FF
    //abnf:            / %x370-37D / %x37F-1FFF / %x200C-200D
    //abnf:            / %x2070-218F / %x2C00-2FEF / %x3001-D7FF
    //abnf:            / %xF900-FDCF / %xFDF0-FFFC / %x10000-EFFFF
    private static boolean isNameStart(int cp) {
        return isAlpha(cp)
                | cp == '_'
                | (cp >= 0x00C0 && cp <= 0x00D6)
                | (cp >= 0x00D8 && cp <= 0x00F6)
                | (cp >= 0x00F8 && cp <= 0x02FF)
                | (cp >= 0x0370 && cp <= 0x037D)
                | (cp >= 0x037F && cp <= 0x1FFF)
                | (cp >= 0x200C && cp <= 0x200D)
                | (cp >= 0x2070 && cp <= 0x218F)
                | (cp >= 0x2C00 && cp <= 0x2FEF)
                | (cp >= 0x3001 && cp <= 0xD7FF)
                | (cp >= 0xF900 && cp <= 0xFDCF)
                | (cp >= 0xFDF0 && cp <= 0xFFFC)
                | (cp >= 0x10000 && cp <= 0xEFFFF);
    }


    private static boolean isAlpha(int cp) {
        return (cp >= 'a' && cp <= 'z') || (cp >= 'Z' && cp <= 'Z');
    }

    //abnf: name-char  = name-start / DIGIT / "-" / "."
    //abnf:            / %xB7 / %x300-36F / %x203F-2040
    private static boolean isNameChar(int cp) {
        return isNameStart(cp)
                | isDigit(cp)
                | cp == '-'
                | cp == '.'
                | cp == 0x00B7
                | (cp >= 0x0300 && cp <= 0x036F)
                | (cp >= 0x203F && cp <= 0x2040);
    }

    private static boolean isDigit(int cp) {
        return cp >= '0' && cp <= '9';
    }

}
