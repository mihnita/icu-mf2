package com.ibm.icu.message2x;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ibm.icu.message2x.MfDataModel.Annotation;
import com.ibm.icu.message2x.MfDataModel.Attribute;
import com.ibm.icu.message2x.MfDataModel.Expression;
import com.ibm.icu.message2x.MfDataModel.FunctionAnnotation;
import com.ibm.icu.message2x.MfDataModel.LiteralOrVariableRef;
import com.ibm.icu.message2x.MfDataModel.Markup;
import com.ibm.icu.message2x.MfDataModel.VariableExpression;
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
                input.backup(1); // let complexBody deal with the wrapping {{ and }}
                ComplexMessage complexBody = getComplexBody();
                spy("complexBody", complexBody);
            } else { // placeholder
                SimpleMessage simpleMessage = getPattern();
                spy("simpleMessage1", simpleMessage);
            }
        } else {
            SimpleMessage simpleMessage = getPattern();
            spy(true, "simpleMessage2", simpleMessage);
        }
        return null;
    }

    static class SimpleMessage {
        final MfDataModel.Pattern parts;
        public SimpleMessage(MfDataModel.Pattern parts) {
            this.parts = parts;
        }
    }

    static class ComplexMessage {
    }

    // abnf: simple-message    = [simple-start pattern]
    // abnf: simple-start      = simple-start-char / text-escape / placeholder
    // abnf: pattern           = *(text-char / text-escape / placeholder)
    private SimpleMessage getPattern() {
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
            case '}': // This is the end, otherwise it would be escaped
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
        int position = input.getPosition();
        int cp = input.peakChar();
        if (cp != '{') {
            return null;
        }
        input.readCodePoint(); // consume the '{'
        skipOptionalWhitespaces();
        cp = input.peakChar();

        MfDataModel.Expression result;
        if (cp == '#' || cp == '/') {
            result = getMarkup();
        } else if (cp == '$') {
            result = getVariableExpression();
        } else if (StringUtils.isFunctionSigil(cp)
                || StringUtils.isPrivateAnnotationSigil(cp)
                || StringUtils.isReservedAnnotationSigil(cp)) {
            result = getAnnotationExpression();
        } else {
            result = getLiteralExpression();
        }
        
        cp = input.readCodePoint(); // consume the '{'
        assertTrue(cp == '}', "Unclosed placeholder");

        return result;
    }

    private Annotation getAnnotationOrMarkdown() {
        MfDataModel.FunctionAnnotation functionAnnotation = null;
        MfDataModel.UnsupportedAnnotation unsupportedAnnotation = null;
        int cp = input.readCodePoint();
        switch (cp) {
            case '}':
                input.backup(1);
                return null;
            case '#':
            case '/':
            case ':': // annotation, function
                //abnf: function       = ":" identifier *(s option)
                String identifier = getIdentifier();
                spy("identifier", identifier);
                List<MfDataModel.Option> options = getOptions();
                spy("options", options);
                functionAnnotation = new MfDataModel.FunctionAnnotation(identifier, options);
                skipOptionalWhitespaces();
                return functionAnnotation;
            default: // reserved && private
                identifier = getIdentifier();
                spy("identifier", identifier);
                String body = getReservedBody();
                spy("reserved-body", body);
                // safe to cast, we already know if it one of the ACII symbols (^&?<>~ etc.) 
                unsupportedAnnotation = new MfDataModel.UnsupportedAnnotation((char) cp, body);
                skipOptionalWhitespaces();
                return unsupportedAnnotation;
        }
    }

    //AICI: literal expression
    //abnf: literal-expression    = "{" [s] literal  [s annotation] *(s attribute) [s] "}"
    private MfDataModel.Expression getLiteralExpression() {
        MfDataModel.Literal literal = getLiteral();
        spy("literal", literal);
        skipOptionalWhitespaces();

        Annotation annotation = getAnnotationOrMarkdown();

        List<MfDataModel.Attribute> attributes = getAttributes();

        skipOptionalWhitespaces();

        // Literal without a function, for example {|hello|} or {123}
        return new MfDataModel.LiteralExpression(literal, annotation, attributes);
    }

    //abnf: variable-expression   = "{" [s] variable [s annotation] *(s attribute) [s] "}"
    private MfDataModel.VariableExpression getVariableExpression() {
        VariableRef variableRef = getVariableRef();
        spy("variableRef", variableRef);
        skipOptionalWhitespaces();

        Annotation annotation = getAnnotationOrMarkdown();
        spy("annotation", annotation);
        skipOptionalWhitespaces();

        List<Attribute> attributes = getAttributes();
        spy("attributes", attributes);
        skipOptionalWhitespaces();
        // Variable without a function, for example {$foo}
        return new MfDataModel.VariableExpression(variableRef, annotation, attributes);
    }

    //AICI Annotation expression
    //abnf: annotation-expression = "{" [s]             annotation  *(s attribute) [s] "}" 
    private MfDataModel.Expression getAnnotationExpression() {
        FunctionAnnotation fa = null;
        Annotation annotation = getAnnotationOrMarkdown();
        if (annotation instanceof FunctionAnnotation) {
            fa = (FunctionAnnotation) annotation;
            skipOptionalWhitespaces();
        }
        List<Attribute> attributes = getAttributes();
        return new MfDataModel.FunctionExpression(fa, attributes);
    }

    //AICI Markdown
    private MfDataModel.Markup getMarkup() {
        int cp = input.readCodePoint();
        if (cp != '#' || cp != '/')
            error("Should not happen. Expecting a markup.");

        Markup.Kind kind = cp == '/' ? Markup.Kind.CLOSE : Markup.Kind.OPEN;

        Annotation annotation = getAnnotationOrMarkdown();
        List<Attribute> attributes = getAttributes();

        cp = input.peakChar();
        if (cp == '/') {
            kind = Markup.Kind.STANDALONE;
            input.readCodePoint();
        }

        if (annotation instanceof FunctionAnnotation) {
            FunctionAnnotation fa = (FunctionAnnotation) annotation;
            return new MfDataModel.Markup(kind, fa.name, fa.options, attributes);
        }

        return null;
    }

    //AICI end refactoring
    
    private List<MfDataModel.Attribute> getAttributes() {
        List<MfDataModel.Attribute> result = new ArrayList<>();
        while (true) {
            MfDataModel.Attribute attribute = getAttribute();
            if (attribute == null) {
                break;
            }
            spy("    attribute", attribute);
            result.add(attribute);
        }
        return result;
    }

    //abnf: attribute      = "@" identifier [[s] "=" [s] (literal / variable)]
    private MfDataModel.Attribute getAttribute() {
        int position = input.getPosition();
        skipOptionalWhitespaces();
        int cp = input.peakChar();
        if (cp == '@') {
            input.readCodePoint(); // consume the '@'
            String id = getIdentifier();
            skipOptionalWhitespaces();
            cp = input.readCodePoint();
            MfDataModel.LiteralOrVariableRef literalOrVariable = null;
            if (cp == '=') {
                skipOptionalWhitespaces();
                literalOrVariable = getLiteralOrVariableRef();
            } else {
                // was not equal, attribute without a value
                input.backup(1);
            }
            return new MfDataModel.Attribute(id, literalOrVariable);
        } else {
            input.gotoPosition(position);
        }
        return null;
    }

    //abnf: reserved-body   = *([s] 1*(reserved-char / reserved-escape / quoted))
    //abnf: reserved-escape = backslash ( backslash / "{" / "|" / "}" )
    private String getReservedBody() {
        StringBuilder result = new StringBuilder();
        // TODO Auto-generated method stub
        while(true) {
            int cp = input.readCodePoint();
            //TODO: whitespace is problematic in the grammar
            if (StringUtils.isReservedChar(cp) || StringUtils.isWhitespace(cp)) {
                result.appendCodePoint(cp);
            } else if (cp == '\\') {
                cp = input.readCodePoint();
                if (cp == '{' || cp == '|' || cp == '}') {
                    result.append(cp);
                } else {
                    error("Invalid escape sequence. Only \\{, \\| and \\} are valid here.");
                }
            } else if (cp == '|') {
                input.backup(1);
                MfDataModel.StringLiteral quoted = (MfDataModel.StringLiteral) getQuotedLiteral();
                result.append(quoted.value);
            } else if (cp == -1) {
                return result.toString();
            } else {
                input.backup(1);
                return result.toString();
            }
        }
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

    private MfDataModel.LiteralOrVariableRef getLiteralOrVariableRef() {
        int cp = input.peakChar();
        if (cp == '$')
            return getVariableRef();
        return getLiteral();
    }

    //abnf: literal        = quoted / unquoted
    private MfDataModel.Literal getLiteral() {
        int cp = input.readCodePoint();
        switch (cp) {
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

    private MfDataModel.VariableRef getVariableRef() {
        int cp = input.readCodePoint();
        if (cp != '$') {
            assertTrue(cp == '$', "We can't get here");
        }

        //abnf: variable       = "$" name
        String name = getName();
        spy("varName", name);
        if (name == null) {
            error("Invalid variable reference following $");
        }
        return new MfDataModel.VariableRef(name);
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
        String numberString = peekWithRegExp(RE_NUMBER_LITERAL);
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

    private ComplexMessage getComplexMessage() {
        List<MfDataModel.Declaration> declarations = new ArrayList<>();
        while (true) {
            MfDataModel.Declaration declaration = getDeclaration();
            if (declaration == null) {
                break;
            }
            declarations.add(declaration);
        }
        spy("declarations", declarations);
        return null;
    }

    //abnf: input-declaration     = input [s] variable-expression
    //abnf: local-declaration     = local s variable [s] "=" [s] expression
    //abnf: reserved-statement    = reserved-keyword [s reserved-body] 1*([s] expression)
    //abnf: reserved-keyword      = "." name
    private MfDataModel.Declaration getDeclaration() {
        int cp = input.readCodePoint();
        if (cp != '.') {
            return null;
        }
        String declName = getName();
        if (declName == null) {
            error("Expected a declaration after the '.'");
            return null;
        }
        switch(declName) {
            case "input":
                skipMandatoryWhitespaces();
                Expression ph = getPlaceholder();
                if (ph instanceof VariableExpression) {
                    return new MfDataModel.InputDeclaration(declName, (VariableExpression) ph);
                }
                break;
            case "local":
                //abnf: local-declaration = local s variable [s] "=" [s] expression
                skipMandatoryWhitespaces();
                LiteralOrVariableRef varName = getVariableRef();
                skipOptionalWhitespaces();
                cp = input.readCodePoint();
                assertTrue(cp == '=', declName);
                skipOptionalWhitespaces();
                Expression expression = getPlaceholder();
                break;
            case "match":
                break;
            default:
                // reserved
        }
        return null;
    }

    // complex-body      = quoted-pattern / matcher
    // quoted-pattern    = "{{" pattern "}}"
    private ComplexMessage getComplexBody() { // {{ ... }}
        int cp = input.readCodePoint();
        assertTrue(cp == '{', "Expected { for a complex body");
        cp = input.readCodePoint();
        assertTrue(cp == '{', "Expected second { for a complex body");
        SimpleMessage pattern = getPattern();
        spy(true, "pattern in complex body", pattern);
        cp = input.readCodePoint();
        assertTrue(cp == '}', "Expected } to end a complex body");
        cp = input.readCodePoint();
        assertTrue(cp == '}', "Expected second } to end a complex body");
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

    private void assertTrue(boolean condition, String message) throws MfException {
        if (!condition) {
            error(message);
        }
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

    private String peekWithRegExp(Pattern pattern) {
        StringView sv = new StringView(input.buffer, input.getPosition());
        Matcher m = pattern.matcher(sv);
        boolean found = m.find();
        if (found) {
            input.skip(m.group().length());
            return m.group();
        }
        return null;
    }

    final static Gson gson = new GsonBuilder()
            //.setPrettyPrinting()
            .create();

    final static boolean DEBUG = true;
    private void spy(String label, Object obj) {
        spy(false, label, obj);
    }

    private void spy(boolean force, String label, Object obj) {
        if (DEBUG) {
            if (force)
                System.out.printf("SPY: %s: %s%n", label, gson.toJson(obj));
            else
                System.out.printf("\033[90mSPY: %s: %s\033[m%n", label, gson.toJson(obj));
        }
//        int position = input.getPosition();
//        System.out.printf("%s: %s // [%d] '%s\u2191%s'%n", label, Objects.toString(obj),
//                position,
//                input.buffer.substring(0, position),
//                input.buffer.substring(position)
//                );
    }
}
