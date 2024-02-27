package com.ibm.icu.message2x;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.message2x.MfDataModel.Attribute;
import com.ibm.icu.message2x.MfDataModel.FunctionAnnotationOrUnsupportedAnnotation;
import com.ibm.icu.message2x.MfDataModel.Literal;
import com.ibm.icu.message2x.MfDataModel.Option;
import com.ibm.icu.message2x.MfDataModel.PatternPart;
import com.ibm.icu.message2x.MfDataModel.UnsupportedAnnotation;
import com.ibm.icu.message2x.MfDataModel.VariableRef;

public class Parser {
    final InputSource input;
    final RegExpTokenizer tokenizer;

    private Parser(String text) {
        this.input = new InputSource(text);
        this.tokenizer = new RegExpTokenizer(input);
    }

    static public MfDataModel.Message parse(String input) {
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
    public MfDataModel.Message parseImpl() {
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
        final MfDataModel.Pattern parts;

        public SimpleMessage(MfDataModel.Pattern parts) {
            this.parts = parts;
        }
        @Override
        public String toString() {
            StringJoiner result = new StringJoiner(",\n    ", "[\n    ", "\n]");
            result.setEmptyValue("[]");
            for (PatternPart part : parts.parts) {
                result.add(part.toString());
            }
            return "SimpleMessage parts: " + result;
        }
    }

    static class ComplexMessage {
    }

    // abnf: simple-message    = [simple-start pattern]
    // abnf: simple-start      = simple-start-char / text-escape / placeholder
    // abnf: pattern           = *(text-char / text-escape / placeholder)
    private SimpleMessage getSimpleMessage() {
        MfDataModel.Pattern parts = new MfDataModel.Pattern();
        while (true) {
            MfDataModel.PatternPart part = getPatternPart();
            if (part == null) {
                break;
            }
            spy("part", part);
            parts.parts.add(part);
        }
        return new SimpleMessage(parts);
    }

    private MfDataModel.PatternPart getPatternPart() {
        int cp = input.peakChar();
        switch (cp) {
            case -1: // EOF
                return null;
            case '{':
                MfDataModel.Expression ph = getPlaceholder();
                spy("placeholder", ph);
                return ph;
            default:
                String plainText = getText();
                MfDataModel.StringPart sp = new MfDataModel.StringPart(plainText);
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
                    if (StringUtils.isContentChar(cp) || StringUtils.isWhitespace(cp) ) {
                        result.appendCodePoint(cp);
                    } else {
                        input.backup(1);
                        return result.toString();
                    }
            }
        }
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
    private MfDataModel.Expression getPlaceholder() {
        int cp = input.readCodePoint();
        if (cp != '{') {
            return null;
        }
        MfDataModel.Expression result = null;
        MfDataModel.LiteralOrVariableRef lvr = null;

        skipOptionalWhitespaces();

        cp = input.readCodePoint();
        String annotationSigils = ":" //abnf: function = ":" identifier *(s option)
                + "^&" //abnf: private-start = "^" / "&"
                + "!%*+<>?~" //abnf: reserved-annotation-start = "!" / "%" / "*" / "+" / "<" / ">" / "?" / "~"
                + "#/" // markup
                ;
        if (annotationSigils.indexOf(cp) == -1) {
            // If it does not start with any of the above sigils it should be a literal or a variable
            //abnf: expression            = literal-expression
            //abnf:                       / variable-expression
            //abnf: literal-expression    = "{" [s] literal [s annotation] *(s attribute) [s] "}"
            //abnf: variable-expression   = "{" [s] variable [s annotation] *(s attribute) [s] "}"
            input.backup(1);
            lvr = getLiteralOrVariableRef();
            spy("lvr", lvr);
            cp = input.peakChar();
            if (cp != '}') {
                skipMandatoryWhitespaces();
                cp = input.readCodePoint();
            }
        }

        switch (cp) {
            case '}':
                break;
            case '#': // open or standalone markup
            case '/': // close markup
                if (lvr != null) {
                    error("Markdown can't have literals or variables");
                }
                Object mk = getMarkup();
                break;
            case ':': // annotation, function
                //abnf: function       = ":" identifier *(s option)
                String identifier = getIdentifier();
                spy("identifier", identifier);
                List<MfDataModel.Option> options = getOptions();
                spy("options", options);
                MfDataModel.FunctionAnnotation fa = new MfDataModel.FunctionAnnotation(identifier, options);
                result = new MfDataModel.FunctionExpression(fa, null);
                skipOptionalWhitespaces();
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
                String body = getReservedBody();
                UnsupportedAnnotation unsup = new UnsupportedAnnotation(cp, body);
                result = new MfDataModel.UnsupportedExpression(unsup, null);
                break;
        }
        // TODO: read attributes
        cp = input.readCodePoint();
        if (cp != '}') {
            error("Placeholder not closed");
        }
        if (result == null && lvr != null) {
            // We found a literal or variable, but no annotations
            // For example ...{$foo}... or ...{|foo bar|} or ...{foo}... 
//            MfDataModel.LiteralOrVariableRef
        }
        return result;
    }

    private String getReservedBody() {
        // TODO Auto-generated method stub
        return null;
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
    private List<MfDataModel.Option> getOptions() {
        List<MfDataModel.Option> options = new ArrayList<>();
        while (true) {
            MfDataModel.Option option = getOption();
            if (option == null) {
                break;
            }
            options.add(option);
        }
        return options;
    }

    //abnf: option         = identifier [s] "=" [s] (literal / variable)
    private MfDataModel.Option getOption() {
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
        MfDataModel.LiteralOrVariableRef litOrVar = getLiteralOrVariableRef();
        return new MfDataModel.Option(identifier, litOrVar);
    }

    //abnf: variable       = "$" name
    //abnf: literal        = quoted / unquoted
    private MfDataModel.LiteralOrVariableRef getLiteralOrVariableRef() {
        int cp = input.readCodePoint();
        switch (cp) {
            case '$': // variableRef
                //abnf: variable       = "$" name
                String name = getName();
                spy("varName", name);
                if (name == null) {
                    error("Invalid variable reference following $");
                }
                return new MfDataModel.VariableRef(name);
            case '|': // quoted
                //abnf: quoted         = "|" *(quoted-char / quoted-escape) "|"
                input.backup(1);
                MfDataModel.Literal ql = getQuotedLiteral();
                spy("QuotedLiteral", ql);
                return ql;
            default : // unquoted
                input.backup(1);
                MfDataModel.Literal unql = getUnQuotedLiteral();
                spy("UnQuotedLiteral", unql);
                return unql;
        }
    }

    private MfDataModel.Literal getQuotedLiteral() {
        StringBuilder result = new StringBuilder();
        int cp = input.readCodePoint();
        if (cp != '|') {
            error("expected starting '|'");
        }
        while (true) {
                cp = input.readCodePoint();
                if (cp == -1) { // EOF
                    break;
                } else if (StringUtils.isQuotedChar(cp)) {
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
        return new MfDataModel.StringLiteral(result.toString());
    }

    private MfDataModel.Literal getUnQuotedLiteral() {
        String name = getName();
        if (name != null) {
            return new MfDataModel.StringLiteral(name);
        }
        return getNumberLiteral();
    }

    //abnf: ; number-literal matches JSON number (https://www.rfc-editor.org/rfc/rfc8259#section-6)
    //abnf: number-literal = ["-"] (%x30 / (%x31-39 *DIGIT)) ["." 1*DIGIT] [%i"e" ["-" / "+"] 1*DIGIT]
    final static Pattern RE_NUMBER_LITERAL = Pattern.compile("^-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+\\-]?[0-9]+)?");
    private MfDataModel.NumberLiteral getNumberLiteral() {
        String numberString = getWithRegExp(RE_NUMBER_LITERAL);
        if (numberString != null) {
            spy("numberString", numberString);
            // TODO: be smarter about it? Integer / Long / Double / BigNumber?
            double value = Double.parseDouble(numberString);
            spy("numberDouble", numberString);
            return new MfDataModel.NumberLiteral(value);
        }
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
            if (!StringUtils.isWhitespace(cp)) {
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
        List<MfDataModel.Declaration> declarations = new ArrayList<>();
        while (true) {
            MfDataModel.Declaration declaration = getDeclaration();
            if (declaration == null) {
                break;
            }
            declarations.add(declaration);
        }
        return null;
    }

    private MfDataModel.Declaration getDeclaration() {
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
        if (!StringUtils.isNameStart(cp)) {
            input.backup(1);
            return null;
        }
        result.appendCodePoint(cp);
        while (true) {
            cp = input.readCodePoint();
            if (StringUtils.isNameChar(cp)) {
                result.appendCodePoint(cp);
            } else {
                input.backup(1);
                break;
            }
        }
        return result.toString();
    }

    private void error(String message) throws MfException {
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

    private String getWithRegExp(Pattern pattern) {
        StringView sv = new StringView(input.buffer, input.getPosition());
        Matcher m = pattern.matcher(sv);
        boolean found = m.find();
        if (found) {
            input.skip(m.group().length());
            return m.group();
        }
        return null;
    }    

    private void spy(String label, Object obj) {
        System.out.printf("SPY: %s: %s%n", label, Objects.toString(obj));
//        int position = input.getPosition();
//        System.out.printf("%s: %s // [%d] '%s\u2191%s'%n", label, Objects.toString(obj),
//                position,
//                input.buffer.substring(0, position),
//                input.buffer.substring(position)
//                );
    }
}
