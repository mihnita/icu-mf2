package com.ibm.icu.message2x;

import java.util.ArrayList;
import java.util.List;

public class MfDataModel {

    private MfDataModel() {
        // Prevent instantiation
    }

    // Messages

    interface Message {}

    static class PatternMessage implements Message {
        final List<Declaration> declarations;
        final Pattern pattern;
        public PatternMessage(List<Declaration> declarations, Pattern pattern) {
            this.declarations = declarations;
            this.pattern = pattern;
        }
    }

    static class SelectMessage implements Message {
        final List<Declaration> declarations;
        final List<Expression> selectors;
        final List<Variant> variants;
        public SelectMessage(List<Declaration> declarations, List<Expression> selectors, List<Variant> variants) {
            this.declarations = declarations;
            this.selectors = selectors;
            this.variants = variants;
        }
    }

    interface Declaration {}

    static class InputDeclaration implements Declaration {
        final String name;
        final VariableExpression value;
        public InputDeclaration(String name, VariableExpression value) {
            this.name = name;
            this.value = value;
        }
    }

    static class LocalDeclaration implements Declaration {
        final String name;
        final Expression value;
        public LocalDeclaration(String name, Expression value) {
            this.name = name;
            this.value = value;
        }
    }

    static class UnsupportedStatement implements Declaration {
        final String keyword;
        final String body;
        final List<Expression> expressions;
        public UnsupportedStatement(String keyword, String body, List<Expression> expressions) {
            this.keyword = keyword;
            this.body = body;
            this.expressions = expressions;
        }
    }

    interface LiteralOrCatchallKey {}

    static class Variant implements LiteralOrCatchallKey {
        final List<LiteralOrCatchallKey> keys;
        final Pattern value;
        public Variant(List<LiteralOrCatchallKey> keys, Pattern value) {
            this.keys = keys;
            this.value = value;
        }
    }

    static class CatchallKey implements LiteralOrCatchallKey {
        // String value; // Always '*' in MF2
    }

    // Patterns

    // type Pattern = Array<string | Expression | Markup>;
    static class Pattern {
        final List<PatternPart> parts;
        Pattern() {
            this.parts = new ArrayList<>();
        }
    }

    interface PatternPart {}

    static class StringPart implements PatternPart {
        final String value;

        StringPart(String value) {
            if (value == null) {
                throw new MfException("StringPart initialized with null");
            }
            this.value = value;
        }
    }

    // type Expression =
    //         | LiteralExpression
    //         | VariableExpression
    //         | FunctionExpression
    //         | UnsupportedExpression
    interface Expression extends PatternPart {}

    static class LiteralExpression implements Expression {
        final Literal arg;
        final Annotation annotation;
        final List<Attribute> attributes;
        public LiteralExpression(Literal arg, Annotation annotation, List<Attribute> attributes) {
            this.arg = arg;
            this.annotation = annotation;
            this.attributes = attributes;
        }
    }

    static class VariableExpression implements Expression {
        final VariableRef arg;
        final Annotation annotation;
        final List<Attribute> attributes;
        public VariableExpression(VariableRef arg, Annotation annotation,
                List<Attribute> attributes) {
            this.arg = arg;
            this.annotation = annotation;
            this.attributes = attributes;
        }
    }

    interface Annotation {}

    static class FunctionExpression implements Expression {
        final FunctionAnnotation annotation;
        final List<Attribute> attributes;
        public FunctionExpression(FunctionAnnotation annotation, List<Attribute> attributes) {
            this.annotation = annotation;
            this.attributes = attributes;
        }
    }

    static class UnsupportedExpression implements Expression {
        final UnsupportedAnnotation annotation;
        final List<Attribute> attributes;
        public UnsupportedExpression(UnsupportedAnnotation annotation, List<Attribute> attributes) {
            this.annotation = annotation;
            this.attributes = attributes;
        }
    }

    static class Attribute {
        final String name;
        final LiteralOrVariableRef value;
        public Attribute(String name, LiteralOrVariableRef value) {
            this.name = name;
            this.value = value;
        }
    }

    // Expressions

    interface LiteralOrVariableRef {}
    interface Literal extends LiteralOrVariableRef, LiteralOrCatchallKey {}

    // Data model feedback: I think this should be StringLiteral
    static class StringLiteral implements Literal {
        final String value;
        public StringLiteral(String value) {
            this.value = value;
        }
    }

    // Not in the official data model
    static class NumberLiteral implements Literal {
        final Number value;
        public NumberLiteral(Number value) {
            this.value = value;
        }
    }

    static class VariableRef implements LiteralOrVariableRef {
        final String name;
        public VariableRef(String name) {
            this.name = name;
        }
    }

    static class FunctionAnnotation implements Annotation {
        final String name;
        final List<Option> options;
        public FunctionAnnotation(String name, List<Option> options) {
            this.name = name;
            this.options = options;
        }
    }

    static class Option {
        final String name;
        final LiteralOrVariableRef value;
        public Option(String name, LiteralOrVariableRef value) {
            this.name = name;
            this.value = value;
        }
    }

    static class UnsupportedAnnotation implements Annotation {
        final String source;
        public UnsupportedAnnotation(String source) {
            this.source = source;
        }
    }

    // Markup

    static class Markup implements Expression {
        enum Kind { OPEN, CLOSE, STANDALONE }
        final Kind kind;
        final String name;
        final List<Option> options;
        final List<Attribute> attributes;
        public Markup(Kind kind, String name, List<Option> options, List<Attribute> attributes) {
            this.kind = kind;
            this.name = name;
            this.options = options;
            this.attributes = attributes;
        }
    }
}
