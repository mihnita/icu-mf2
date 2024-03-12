// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2x;

import com.ibm.icu.message2x.MFDataModel.Annotation;
import com.ibm.icu.message2x.MFDataModel.Attribute;
import com.ibm.icu.message2x.MFDataModel.CatchallKey;
import com.ibm.icu.message2x.MFDataModel.Declaration;
import com.ibm.icu.message2x.MFDataModel.Expression;
import com.ibm.icu.message2x.MFDataModel.FunctionAnnotation;
import com.ibm.icu.message2x.MFDataModel.FunctionExpression;
import com.ibm.icu.message2x.MFDataModel.InputDeclaration;
import com.ibm.icu.message2x.MFDataModel.Literal;
import com.ibm.icu.message2x.MFDataModel.LiteralExpression;
import com.ibm.icu.message2x.MFDataModel.LiteralOrCatchallKey;
import com.ibm.icu.message2x.MFDataModel.LiteralOrVariableRef;
import com.ibm.icu.message2x.MFDataModel.LocalDeclaration;
import com.ibm.icu.message2x.MFDataModel.Markup;
import com.ibm.icu.message2x.MFDataModel.Option;
import com.ibm.icu.message2x.MFDataModel.Pattern;
import com.ibm.icu.message2x.MFDataModel.PatternMessage;
import com.ibm.icu.message2x.MFDataModel.PatternPart;
import com.ibm.icu.message2x.MFDataModel.SelectMessage;
import com.ibm.icu.message2x.MFDataModel.StringPart;
import com.ibm.icu.message2x.MFDataModel.UnsupportedAnnotation;
import com.ibm.icu.message2x.MFDataModel.UnsupportedExpression;
import com.ibm.icu.message2x.MFDataModel.UnsupportedStatement;
import com.ibm.icu.message2x.MFDataModel.VariableExpression;
import com.ibm.icu.message2x.MFDataModel.VariableRef;
import com.ibm.icu.message2x.MFDataModel.Variant;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

// TODO: find a better name for this class
public class MFSerializer {
    private boolean shouldDoubleQuotePattern = false;
    private boolean needSpace = false;
    private final StringBuilder result = new StringBuilder();

    public static String dataModelToString(MFDataModel.Message message) {
        return new MFSerializer().messageToString(message);
    }

    private String messageToString(MFDataModel.Message message) {
        if (message instanceof PatternMessage) {
            patternMessageToString((PatternMessage) message);
        } else if (message instanceof SelectMessage) {
            selectMessageToString((SelectMessage) message);
        } else {
            errorType("Message", message);
        }
        return result.toString();
    }

    private void selectMessageToString(SelectMessage message) {
        declarationsToString(message.declarations);
        shouldDoubleQuotePattern = true;
        addSpaceIfNeeded();
        result.append(".match");
        for (Expression selector : message.selectors) {
            result.append(' ');
            expressionToString(selector);
        }
        for (Variant variant : message.variants) {
            variantToString(variant);
        }
    }

    private void patternMessageToString(PatternMessage message) {
        declarationsToString(message.declarations);
        patternToString(message.pattern);
    }

    private void patternToString(Pattern pattern) {
        addSpaceIfNeeded();
        if (shouldDoubleQuotePattern) {
            result.append("{{");
        }
        for (PatternPart part : pattern.parts) {
            if (part instanceof StringPart) {
                stringPartToString((StringPart) part);
            } else {
                expressionToString((Expression) part);
            }
        }
        if (shouldDoubleQuotePattern) {
            result.append("}}");
        }
    }

    private void expressionToString(Expression expression) {
        if (expression == null) {
            return;
        }
        if (expression instanceof LiteralExpression) {
            literalExpressionToString((LiteralExpression) expression);
        } else if (expression instanceof VariableExpression) {
            variableExpressionToString((VariableExpression) expression);
        } else if (expression instanceof FunctionExpression) {
            functionExpressionToString((FunctionExpression) expression);
        } else if (expression instanceof Markup) {
            markupToString((Markup) expression);
        } else if (expression instanceof UnsupportedExpression) {
            unsupportedExpressionToString((UnsupportedExpression) expression);
        } else {
            errorType("Expression", expression);
        }
    }

    private void unsupportedExpressionToString(UnsupportedExpression ue) {
        result.append('{');
        annotationToString(ue.annotation);
        attributesToString(ue.attributes);
        result.append('}');
    }

    private void markupToString(Markup markup) {
        result.append('{');
        if (markup.kind == Markup.Kind.CLOSE) {
            result.append('/');
        } else {
            result.append('#');
        }
        result.append(markup.name);
        optionsToString(markup.options);
        attributesToString(markup.attributes);
        if (markup.kind == Markup.Kind.STANDALONE) {
            result.append('/');
        }
        result.append('}');
    }

    private void optionsToString(Map<String, Option> options) {
        for (Option option : options.values()) {
            result.append(' ');
            result.append(option.name);
            result.append('=');
            literalOrVariableRefToString(option.value);
        }
    }

    private void functionExpressionToString(FunctionExpression fe) {
        result.append('{');
        annotationToString(fe.annotation);
        attributesToString(fe.attributes);
        result.append('}');
    }

    private void attributesToString(List<Attribute> attributes) {
        if (attributes == null) {
            return;
        }
        for (Attribute attribute : attributes) {
            result.append(" @");
            result.append(attribute.name);
            // Attributes can be with without a value (for now?)
            if (attribute.value != null) {
                result.append('=');
                literalOrVariableRefToString(attribute.value);
            }
        }
    }

    private void annotationToString(Annotation annotation) {
        if (annotation == null) {
            return;
        }
        if (annotation instanceof FunctionAnnotation) {
            addSpaceIfNeeded();
            result.append(":");
            result.append(((FunctionAnnotation) annotation).name);
            optionsToString(((FunctionAnnotation) annotation).options);
        } else if (annotation instanceof UnsupportedAnnotation) {
            addSpaceIfNeeded();
            String value = ((UnsupportedAnnotation) annotation).source;
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '\\' || c == '{' || c == '}') {
                    result.append('\\');
                }
                result.append(c);
            }
        } else {
            errorType("Annotation", annotation);
        }
    }

    private void variableExpressionToString(VariableExpression ve) {
        if (ve == null) {
            return;
        }
        result.append('{');
        literalOrVariableRefToString(ve.arg);
        needSpace = true;
        annotationToString(ve.annotation);
        attributesToString(ve.attributes);
        result.append('}');
        needSpace = false;
    }

    private void literalOrVariableRefToString(LiteralOrVariableRef literalOrVarRef) {
        if (literalOrVarRef instanceof Literal) {
            literalToString((Literal) literalOrVarRef);
        } else if (literalOrVarRef instanceof VariableRef) {
            result.append("$" + ((VariableRef) literalOrVarRef).name);
        } else {
            errorType("LiteralOrVariableRef", literalOrVarRef);
        }
    }

    // abnf: number-literal = ["-"] (%x30 / (%x31-39 *DIGIT)) ["." 1*DIGIT]
    // [%i"e" ["-" / "+"] 1*DIGIT]
    // Not identical to the one in the parser. This one has a $ at the end, to
    // match the whole string
    // TBD if it can be refactored to reuse.
    private static final java.util.regex.Pattern RE_NUMBER_LITERAL =
            java.util.regex.Pattern.compile("^-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+\\-]?[0-9]+)?$");

    private void literalToString(Literal literal) {
        String value = literal.value;
        Matcher matcher = RE_NUMBER_LITERAL.matcher(value);
        if (matcher.find()) { // It is a number, output as is
            result.append(value);
        } else {
            StringBuilder literalBuffer = new StringBuilder();
            boolean wasName = true;
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '\\' || c == '|') {
                    literalBuffer.append('\\');
                }
                literalBuffer.append(c);
                if (i == 0 && !StringUtils.isNameStart(c)) {
                    wasName = false;
                } else if (!StringUtils.isNameChar(c)) {
                    wasName = false;
                }
            }
            if (wasName && literalBuffer.length() != 0) {
                result.append(literalBuffer);
            } else {
                result.append('|');
                result.append(literalBuffer);
                result.append('|');
            }
        }
    }

    private void literalExpressionToString(LiteralExpression le) {
        result.append('{');
        literalOrVariableRefToString(le.arg);
        needSpace = true;
        annotationToString(le.annotation);
        attributesToString(le.attributes);
        result.append('}');
    }

    private void stringPartToString(StringPart part) {
        if (part.value.startsWith(".")) {
            if (!shouldDoubleQuotePattern) {
                shouldDoubleQuotePattern = true;
                result.append("{{");
            }
        }
        for (int i = 0; i < part.value.length(); i++) {
            char c = part.value.charAt(i);
            if (c == '\\' || c == '{' || c == '}') {
                result.append('\\');
            }
            result.append(c);
        }
    }

    private void declarationsToString(List<Declaration> declarations) {
        if (declarations == null || declarations.isEmpty()) {
            return;
        }
        shouldDoubleQuotePattern = true;
        for (Declaration declaration : declarations) {
            if (declaration instanceof LocalDeclaration) {
                localDeclarationToString((LocalDeclaration) declaration);
            } else if (declaration instanceof InputDeclaration) {
                inputDeclarationToString((InputDeclaration) declaration);
            } else if (declaration instanceof UnsupportedStatement) {
                unsupportedStatementToString((UnsupportedStatement) declaration);
            } else {
                errorType("Declaration", declaration);
            }
        }
    }

    private void unsupportedStatementToString(UnsupportedStatement declaration) {
        addSpaceIfNeeded();
        result.append('.');
        result.append(declaration.keyword);
        if (!declaration.body.isEmpty()) {
            result.append(' ');
        }
        result.append('|');
        result.append(declaration.body);
        result.append('|');
        needSpace = true;
        for (Expression expression : declaration.expressions) {
            addSpaceIfNeeded();
            expressionToString(expression);
            needSpace = true;
        }
    }

    private void inputDeclarationToString(InputDeclaration declaration) {
        addSpaceIfNeeded();
        result.append(".input ");
        variableExpressionToString(declaration.value);
        needSpace = true;
    }

    private void localDeclarationToString(LocalDeclaration declaration) {
        addSpaceIfNeeded();
        result.append(".local $");
        result.append(declaration.name);
        result.append(" = ");
        expressionToString(declaration.value);
        needSpace = true;
    }

    private void variantToString(Variant variant) {
        for (LiteralOrCatchallKey key : variant.keys) {
            result.append(' ');
            if (key instanceof CatchallKey) {
                result.append('*');
            } else {
                literalToString(((Literal) key));
            }
        }
        result.append(' ');
        patternToString(variant.value);
    }

    private void addSpaceIfNeeded() {
        if (needSpace) {
            result.append(' ');
            needSpace = false;
        }
    }

    private void errorType(String expectedType, Object obj) {
        error("Unexpected '" + expectedType + "' type: ", obj);
    }

    private void error(String text, Object obj) {
        error(text + obj.getClass().getName());
    }

    private void error(String text) {
        throw new RuntimeException(text);
    }
}
