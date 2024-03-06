// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2x;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

class MfParser {
    private static final int EOF = -1;
    private final InputSource input;

    MfParser(String text) {
        this.input = new InputSource(text);
    }

    public static MfDataModel.Message parse(String input) {
        return new MfParser(input).parseImpl();
    }

    // Parser proper
    private MfDataModel.Message parseImpl() {
        MfDataModel.Message result;
        int cp = input.peekChar();
        if (cp == '.') { // declarations or .match
            result = getComplexMessage();
            spy("complexMessage", result);
        } else if (cp == '{') { // `{` or `{{`
            cp = input.readCodePoint();
            cp = input.peekChar();
            if (cp == '{') { // `{{`, complex body without declarations
                input.backup(1); // let complexBody deal with the wrapping {{ and }}
                MfDataModel.Pattern pattern = getQuotedPattern();
                spy("simpleMessage wrapped in {{...}}", pattern);
                result = new MfDataModel.PatternMessage(new ArrayList<>(), pattern);
            } else { // placeholder
                input.backup(1); // We want the '{' present, to detect the part as placeholder.
                MfDataModel.Pattern pattern = getPattern();
                spy("simpleMessage starting with a placeholder", pattern);
                result = new MfDataModel.PatternMessage(new ArrayList<>(), pattern);
            }
        } else {
            MfDataModel.Pattern pattern = getPattern();
            spy("simpleMessage2", pattern);
            result = new MfDataModel.PatternMessage(new ArrayList<>(), pattern);
        }
        skipOptionalWhitespaces();
        checkCondition(input.atEnd(), "Content detected after the end of the message.");
        spy(true, "message", result);
        return result;
    }

    // abnf: simple-message = [simple-start pattern]
    // abnf: simple-start = simple-start-char / text-escape / placeholder
    // abnf: pattern = *(text-char / text-escape / placeholder)
    private MfDataModel.Pattern getPattern() {
        MfDataModel.Pattern pattern = new MfDataModel.Pattern();
        while (true) {
            MfDataModel.PatternPart part = getPatternPart();
            if (part == null) {
                break;
            }
            spy("part", part);
            pattern.parts.add(part);
        }
//        checkCondition(!pattern.parts.isEmpty(), "Empty pattern");
        return pattern;
    }

    private MfDataModel.PatternPart getPatternPart() {
        int cp = input.peekChar();
        switch (cp) {
            case EOF:
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
                case EOF:
                    return result.toString();
                case '\\':
                    cp = input.readCodePoint();
                    if (cp == '\\' || cp == '{' || cp == '|' | cp == '}') {
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
                    if (StringUtils.isContentChar(cp) || StringUtils.isWhitespace(cp)) {
                        result.appendCodePoint(cp);
                    } else {
                        input.backup(1);
                        return result.toString();
                    }
            }
        }
    }

    // abnf: placeholder = expression / markup
    // abnf: expression = literal-expression
    // abnf: / variable-expression
    // abnf: / annotation-expression
    // abnf: literal-expression = "{" [s] literal [s annotation] *(s attribute)
    // [s] "}"
    // abnf: variable-expression = "{" [s] variable [s annotation] *(s
    // attribute) [s] "}"
    // abnf: annotation-expression = "{" [s] annotation *(s attribute) [s] "}"

    // abnf: markup = "{" [s] "#" identifier *(s option) *(s attribute) [s]
    // ["/"] "}" ; open and standalone
    // abnf: / "{" [s] "/" identifier *(s option) *(s attribute) [s] "}" ; close
    private MfDataModel.Expression getPlaceholder() {
        int cp = input.peekChar();
        if (cp != '{') {
            return null;
        }
        input.readCodePoint(); // consume the '{'
        skipOptionalWhitespaces();
        cp = input.peekChar();

        MfDataModel.Expression result;
        if (cp == '#' || cp == '/') {
            result = getMarkup();
        } else if (cp == '$') {
            result = getVariableExpression();
        } else if (StringUtils.isFunctionSigil(cp) || StringUtils.isPrivateAnnotationSigil(cp)
                || StringUtils.isReservedAnnotationSigil(cp)) {
            result = getAnnotationExpression();
        } else {
            result = getLiteralExpression();
        }

        skipOptionalWhitespaces();
        cp = input.readCodePoint(); // consume the '}'
        checkCondition(cp == '}', "Unclosed placeholder");

        return result;
    }

    private MfDataModel.Annotation getAnnotationOrMarkup() {
        int position = input.getPosition();
        skipOptionalWhitespaces();

        int cp = input.peekChar();
        switch (cp) {
            case '}':
                return null;
            case '#':
            case '/':
            case ':': // annotation, function
                // abnf: function = ":" identifier *(s option)
                input.readCodePoint(); // Consume the sigil
                String identifier = getIdentifier();
                spy("identifier", identifier);
                checkCondition(identifier != null, "Annotation / function name missing");
                List<MfDataModel.Option> options = getOptions();
                spy("options", options);
                return new MfDataModel.FunctionAnnotation(identifier, options);
            default: // reserved && private
                if (StringUtils.isReservedAnnotationSigil(cp) || StringUtils.isPrivateAnnotationSigil(cp)) {
                    identifier = getIdentifier();
                    spy("identifier", identifier);
                    String body = getReservedBody();
                    spy("reserved-body", body);
                    // The sigil is part of the body.
                    // It is safe to cast, we know it is in the BMP
                    body = (char) (cp) + body;
                    // Safe to cast, we already know if it one of the ACII symbols (^&?<>~ etc.)
                    return new MfDataModel.UnsupportedAnnotation(body);
                }
        }
        input.gotoPosition(position);
        return null;
    }

    // abnf: literal-expression = "{" [s] literal [s annotation] *(s attribute) [s] "}"
    private MfDataModel.Expression getLiteralExpression() {
        MfDataModel.Literal literal = getLiteral();
        spy("literal", literal);
        checkCondition(literal != null, "Literal expression expected.");

        MfDataModel.Annotation annotation = null;
        int wsCount = skipWhitespaces();
        if (wsCount > 0) { // we might have an annotation
            annotation = getAnnotationOrMarkup();
            if (annotation == null) {
                // We had some spaces, but no annotation.
                // So we put (some) back for the possible attributes.
                input.backup(1);
            }
        }

        List<MfDataModel.Attribute> attributes = getAttributes();

        // skipOptionalWhitespaces();

        // Literal without a function, for example {|hello|} or {123}
        return new MfDataModel.LiteralExpression(literal, annotation, attributes);
    }

    // abnf: variable-expression = "{" [s] variable [s annotation] *(s attribute) [s] "}"
    private MfDataModel.VariableExpression getVariableExpression() {
        MfDataModel.VariableRef variableRef = getVariableRef();
        spy("variableRef", variableRef);
//        skipOptionalWhitespaces();

        MfDataModel.Annotation annotation = getAnnotationOrMarkup();
        spy("annotation", annotation);
//        skipOptionalWhitespaces();

        List<MfDataModel.Attribute> attributes = getAttributes();
        spy("attributes", attributes);
//        skipOptionalWhitespaces();
        // Variable without a function, for example {$foo}
        return new MfDataModel.VariableExpression(variableRef, annotation, attributes);
    }

    // abnf: annotation-expression = "{" [s] annotation *(s attribute) [s] "}"
    private MfDataModel.Expression getAnnotationExpression() {
        MfDataModel.FunctionAnnotation fa = null;
        MfDataModel.Annotation annotation = getAnnotationOrMarkup();
        if (annotation instanceof MfDataModel.FunctionAnnotation) {
            fa = (MfDataModel.FunctionAnnotation) annotation;
            skipOptionalWhitespaces();
        }
        List<MfDataModel.Attribute> attributes = getAttributes();
        return new MfDataModel.FunctionExpression(fa, attributes);
    }

    // abnf: markup = "{" [s] "#" identifier *(s option) *(s attribute) [s] ["/"] "}" ; open and standalone
    // abnf: / "{" [s] "/" identifier *(s option) *(s attribute) [s] "}" ; close
    private MfDataModel.Markup getMarkup() {
        int cp = input.peekChar(); // consume the '{'
        checkCondition(cp == '#' || cp == '/', "Should not happen. Expecting a markup.");

        MfDataModel.Markup.Kind kind = cp == '/'
                ? MfDataModel.Markup.Kind.CLOSE
                : MfDataModel.Markup.Kind.OPEN;

        MfDataModel.Annotation annotation = getAnnotationOrMarkup();
        List<MfDataModel.Attribute> attributes = getAttributes();

        cp = input.peekChar();
        if (cp == '/') {
            kind = MfDataModel.Markup.Kind.STANDALONE;
            input.readCodePoint();
        }

        if (annotation instanceof MfDataModel.FunctionAnnotation) {
            MfDataModel.FunctionAnnotation fa = (MfDataModel.FunctionAnnotation) annotation;
            return new MfDataModel.Markup(kind, fa.name, fa.options, attributes);
        }

        return null;
    }

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

    // abnf: attribute = "@" identifier [[s] "=" [s] (literal / variable)]
    private MfDataModel.Attribute getAttribute() {
        int position = input.getPosition();
        if (skipWhitespaces() == 0) {
            input.gotoPosition(position);
            return null;
        }
        int cp = input.peekChar();
        if (cp == '@') {
            input.readCodePoint(); // consume the '@'
            String id = getIdentifier();
            int wsCount = skipWhitespaces();
            cp = input.peekChar();
            MfDataModel.LiteralOrVariableRef literalOrVariable = null;
            if (cp == '=') {
                input.readCodePoint();
                skipOptionalWhitespaces();
                literalOrVariable = getLiteralOrVariableRef();
                checkCondition(literalOrVariable != null, "Attributes must have a value after `=`");
            } else {
                // was not equal, attribute without a value, put the "spaces" back.
                input.backup(wsCount);
            }
            return new MfDataModel.Attribute(id, literalOrVariable);
        } else {
            input.gotoPosition(position);
        }
        return null;
    }

    // abnf: reserved-body = *([s] 1*(reserved-char / reserved-escape / quoted))
    // abnf: reserved-escape = backslash ( backslash / "{" / "|" / "}" )
    private String getReservedBody() {
        StringBuilder result = new StringBuilder();
        // TODO Auto-generated method stub
        while (true) {
            int cp = input.readCodePoint();
            // TODO: whitespace is problematic in the grammar
            if (StringUtils.isReservedChar(cp) || StringUtils.isWhitespace(cp)) {
                result.appendCodePoint(cp);
            } else if (cp == '\\') {
                cp = input.readCodePoint();
                checkCondition(cp == '{' || cp == '|' || cp == '}',
                        "Invalid escape sequence. Only \\{, \\| and \\} are valid here.");
                result.append(cp);
            } else if (cp == '|') {
                input.backup(1);
                MfDataModel.Literal quoted = getQuotedLiteral();
                result.append(quoted.value);
            } else if (cp == EOF) {
                return result.toString();
            } else {
                input.backup(1);
                return result.toString();
            }
        }
    }

    // abnf: identifier = [namespace ":"] name
    // abnf: namespace = name
    // abnf: name = name-start *name-char
    private String getIdentifier() {
        String namespace = getName();
        if (namespace == null) {
            return null;
        }
        int cp = input.readCodePoint();
        if (cp == ':') { // the previous name was namespace
            String name = getName();
            checkCondition(name != null, "Expected name after namespace '" + namespace + "'");
            return namespace + ":" + name;
        } else {
            input.backup(1);
        }
        return namespace;
    }

    // abnf helper: *(s option)
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

    // abnf: option = identifier [s] "=" [s] (literal / variable)
    private MfDataModel.Option getOption() {
        int position = input.getPosition();
        skipOptionalWhitespaces();
        String identifier = getIdentifier();
        if (identifier == null) {
            input.gotoPosition(position);
            return null;
        }
        skipOptionalWhitespaces();
        int cp = input.readCodePoint();
        checkCondition(cp == '=', "Expected '='");
//        skipOptionalWhitespaces();
        MfDataModel.LiteralOrVariableRef litOrVar = getLiteralOrVariableRef();
        return new MfDataModel.Option(identifier, litOrVar);
    }

    private MfDataModel.LiteralOrVariableRef getLiteralOrVariableRef() {
        int cp = input.peekChar();
        if (cp == '$') {
            return getVariableRef();
        }
        return getLiteral();
    }

    // abnf: literal = quoted / unquoted
    private MfDataModel.Literal getLiteral() {
        int cp = input.readCodePoint();
        switch (cp) {
            case '|': // quoted
                // abnf: quoted = "|" *(quoted-char / quoted-escape) "|"
                input.backup(1);
                MfDataModel.Literal ql = getQuotedLiteral();
                spy("QuotedLiteral", ql);
                return ql;
            default: // unquoted
                input.backup(1);
                MfDataModel.Literal unql = getUnQuotedLiteral();
                spy("UnQuotedLiteral", unql);
                return unql;
        }
    }

    private MfDataModel.VariableRef getVariableRef() {
        int cp = input.readCodePoint();
        if (cp != '$') {
            checkCondition(cp == '$', "We can't get here");
        }

        // abnf: variable = "$" name
        String name = getName();
        spy("varName", name);
        checkCondition(name != null, "Invalid variable reference following $");
        return new MfDataModel.VariableRef(name);
    }

    private MfDataModel.Literal getQuotedLiteral() {
        StringBuilder result = new StringBuilder();
        int cp = input.readCodePoint();
        checkCondition(cp == '|', "expected starting '|'");
        while (true) {
            cp = input.readCodePoint();
            if (cp == EOF) {
                break;
            } else if (StringUtils.isQuotedChar(cp)) {
                result.appendCodePoint(cp);
            } else if (cp == '\\') {
                cp = input.readCodePoint();
                checkCondition(cp == '|', "Invalid escape sequence, only \"\\|\" is valid here");
                result.appendCodePoint('|');
            } else {
                break;
            }
        }

        checkCondition(cp == '|', "expected ending '|'");

        return new MfDataModel.Literal(result.toString());
    }

    private MfDataModel.Literal getUnQuotedLiteral() {
        String name = getName();
        if (name != null) {
            return new MfDataModel.Literal(name);
        }
        return getNumberLiteral();
    }

    // abnf: ; number-literal matches JSON number (https://www.rfc-editor.org/rfc/rfc8259#section-6)
    // abnf: number-literal = ["-"] (%x30 / (%x31-39 *DIGIT)) ["." 1*DIGIT] [%i"e" ["-" / "+"] 1*DIGIT]
    private static final Pattern RE_NUMBER_LITERAL = Pattern.compile("^-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+\\-]?[0-9]+)?");

    private MfDataModel.Literal getNumberLiteral() {
        String numberString = peekWithRegExp(RE_NUMBER_LITERAL);
        if (numberString != null) {
            spy("numberString", numberString);
            return new MfDataModel.Literal(numberString);
        }
        return null;
    }

    private void skipMandatoryWhitespaces() {
        int count = skipWhitespaces();
        checkCondition(count > 0, "Space expected");
    }

    private void skipOptionalWhitespaces() {
        skipWhitespaces();
    }

    private int skipWhitespaces() {
        int skipCount = 0;
        while (true) {
            int cp = input.readCodePoint();
            if (cp == EOF) {
                return skipCount;
            }
            if (!StringUtils.isWhitespace(cp)) {
                input.backup(1);
                return skipCount;
            }
            skipCount++;
        }
    }

    private MfDataModel.Message getComplexMessage() {
        List<MfDataModel.Declaration> declarations = new ArrayList<>();
        boolean foundMatch = false;
        while (true) {
            MfDataModel.Declaration declaration = getDeclaration();
            if (declaration == null) {
                break;
            }
            if (declaration instanceof MatchDeclaration) {
                foundMatch = true;
                break;
            }
            declarations.add(declaration);
        }
        if (foundMatch) {
            return getMatch(declarations);
        } else { // Expect {{...}} or end of message
            skipOptionalWhitespaces();
            int cp = input.peekChar();
            if (cp == EOF) {
                // Only declarations, no pattern
                return new MfDataModel.PatternMessage(declarations, null);
            } else {
                MfDataModel.Pattern pattern = getQuotedPattern();
                return new MfDataModel.PatternMessage(declarations, pattern);
            }
        }
    }

    // abnf: matcher = match-statement 1*([s] variant)
    // abnf: match-statement = match 1*([s] selector)
    // abnf: selector = expression
    // abnf: variant = key *(s key) [s] quoted-pattern
    // abnf: key = literal / "*"
    // abnf: match = %s".match"
    private MfDataModel.SelectMessage getMatch(List<MfDataModel.Declaration> declarations) {
        // ".match" was already consumed by the caller
        // Look for selectors
        List<MfDataModel.Expression> expressions = new ArrayList<>();
        while (true) {
            skipMandatoryWhitespaces();
            MfDataModel.Expression expression = getPlaceholder();
            spy("selector expression", expression);
            if (expression == null) {
                break;
            }
            checkCondition(! (expression instanceof MfDataModel.Markup), "Cannot do selection on markup");
            expressions.add(expression);
        }

        checkCondition(!expressions.isEmpty(), "There should be at least one selector expression.");

        // At this point we need to look for variants, which are key - value
        List<MfDataModel.Variant> variants = new ArrayList<>();
        while (true) {
            MfDataModel.Variant variant = getVariant();
            spy("variant", variant);
            if (variant == null) {
                break;
            }
            variants.add(variant);
        }
        return new MfDataModel.SelectMessage(declarations, expressions, variants);
    }

    // abnf: variant = key *(s key) [s] quoted-pattern
    // abnf: key = literal / "*"
    private MfDataModel.Variant getVariant() {
        List<MfDataModel.LiteralOrCatchallKey> keys = new ArrayList<>();
        while (true) {
            MfDataModel.LiteralOrCatchallKey key = getKey();
            spy("key", key);
            if (key == null) {
                break;
            }
            keys.add(key);
        }
        spy("keys", keys);
        skipOptionalWhitespaces();
        if (input.atEnd()) {
            checkCondition(keys.isEmpty(), "After selector keys it is mandatory to have a pattern.");
            return null;
        }
        MfDataModel.Pattern pattern = getQuotedPattern();
        spy("quoted pattern", pattern);
        return new MfDataModel.Variant(keys, pattern);
    }

    private MfDataModel.LiteralOrCatchallKey getKey() {
        skipOptionalWhitespaces();
        int cp = input.peekChar();
        if (cp == '*') {
            input.readCodePoint(); // consume the '*'
            return new MfDataModel.CatchallKey();
        }
        if (cp == EOF) {
            return null;
        }
        return getLiteral();
    }

    private static class MatchDeclaration implements MfDataModel.Declaration {
        // Provides a common type for XXX
    }

    // abnf: input-declaration = input [s] variable-expression
    // abnf: local-declaration = local s variable [s] "=" [s] expression
    // abnf: reserved-statement = reserved-keyword [s reserved-body] 1*([s] expression)
    // abnf: reserved-keyword = "." name
    private MfDataModel.Declaration getDeclaration() {
        int position = input.getPosition();
        skipOptionalWhitespaces();
        int cp = input.readCodePoint();
        if (cp != '.') {
            input.gotoPosition(position);
            return null;
        }
        String declName = getName();
        checkCondition(declName != null, "Expected a declaration after the '.'");

        MfDataModel.Expression expression;
        switch (declName) {
            case "input":
                skipMandatoryWhitespaces();
                expression = getPlaceholder();
                if (expression instanceof MfDataModel.VariableExpression) {
                    return new MfDataModel.InputDeclaration(declName, (MfDataModel.VariableExpression) expression);
                }
                break;
            case "local":
                // abnf: local-declaration = local s variable [s] "=" [s] expression
                skipMandatoryWhitespaces();
                MfDataModel.LiteralOrVariableRef varName = getVariableRef();
                skipOptionalWhitespaces();
                cp = input.readCodePoint();
                checkCondition(cp == '=', declName);
                skipOptionalWhitespaces();
                expression = getPlaceholder();
                if (varName instanceof MfDataModel.VariableRef) {
                    return new MfDataModel.LocalDeclaration(((MfDataModel.VariableRef) varName).name, expression);
                }
                break;
            case "match":
                return new MatchDeclaration();
            default: // abnf: reserved-statement = reserved-keyword [s reserved-body] 1*([s] expression)
                skipOptionalWhitespaces();
                String body = getReservedBody();
                List<MfDataModel.Expression> expressions = new ArrayList<>();
                while (true) {
                    skipOptionalWhitespaces();
                    expression = getPlaceholder();
                    // This also covers != null
                    if (expression instanceof MfDataModel.VariableExpression) {
                        expressions.add(expression);
                    } else {
                        break;
                    }
                }
                return new MfDataModel.UnsupportedStatement(declName, body, expressions);
        }
        return null;
    }

    // quoted-pattern = "{{" pattern "}}"
    private MfDataModel.Pattern getQuotedPattern() { // {{ ... }}
        int cp = input.readCodePoint();
        checkCondition(cp == '{', "Expected { for a complex body");
        cp = input.readCodePoint();
        checkCondition(cp == '{', "Expected second { for a complex body");
        MfDataModel.Pattern pattern = getPattern();
        spy("pattern in complex body", pattern);
        cp = input.readCodePoint();
        checkCondition(cp == '}', "Expected } to end a complex body");
        cp = input.readCodePoint();
        checkCondition(cp == '}', "Expected second } to end a complex body");
        return pattern;
    }

    private String getName() {
        StringBuilder result = new StringBuilder();
        int cp = input.readCodePoint();
        checkCondition(cp != EOF, "Expected name or namespace.");
        if (!StringUtils.isNameStart(cp)) {
            input.backup(1);
            return null;
        }
        result.appendCodePoint(cp);
        while (true) {
            cp = input.readCodePoint();
            if (StringUtils.isNameChar(cp)) {
                result.appendCodePoint(cp);
            } else if (cp == EOF) {
                break;
            } else {
                input.backup(1);
                break;
            }
        }
        return result.toString();
    }

    private void checkCondition(boolean condition, String message) throws MfParseException {
        if (!condition) {
            error(message);
        }
    }

    private void error(String message) throws MfParseException {
        StringBuilder finalMsg = new StringBuilder();
        if (input == null) {
            finalMsg.append("Parse error: ");
            finalMsg.append(message);
        } else {
            int position = input.getPosition();
            finalMsg.append("Parse error [" + input.getPosition() + "]: ");
            finalMsg.append(message);
            finalMsg.append("\n");
            if (position != EOF) {
                finalMsg.append(input.buffer.substring(0, position));
                finalMsg.append("^^^");
                finalMsg.append(input.buffer.substring(position));
            } else {
                finalMsg.append(input.buffer);
                finalMsg.append("^^^");
            }
        }
        throw new MfParseException(finalMsg.toString());
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

    // TODO: Debug utilities, to remove

    private static final Gson GSON = new GsonBuilder()
            // .setPrettyPrinting()
            .setDateFormat("yyyyMMdd'T'HHmmss").create();

    static boolean debug = true;

    private void spy(String label, Object obj) {
        spy(false, label, obj);
    }

    private void spy(boolean force, String label, Object obj) {
        if (debug) {
            int position = input.getPosition();
            if (force) {
                System.out.printf("SPY: %s: %s%n", label, GSON.toJson(obj));
                System.out.printf("%s\u2191\u2191\u2191%s%n", input.buffer.substring(0, position), input.buffer.substring(position));
            } else {
                System.out.printf("\033[90mSPY: %s: %s\033[m%n", label, GSON.toJson(obj));
                System.out.printf("\033[90m%s\u2191\u2191\u2191%s\033[m%n", input.buffer.substring(0, position), input.buffer.substring(position));
            }
        }
    }
}
