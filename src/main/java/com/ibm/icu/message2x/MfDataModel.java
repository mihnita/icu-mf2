package com.ibm.icu.message2x;

import java.util.ArrayList;
import java.util.List;

public class MfDataModel {

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
        String name;
        VariableExpression value;
    }

    static class LocalDeclaration implements Declaration {
        String name;
        Expression value;
    }

    static class UnsupportedStatement implements Declaration {
        String keyword;
        String body;
        List<Expression> expressions;
    }

    interface LiteralOrCatchallKey {}

    static class Variant implements LiteralOrCatchallKey {
        List<LiteralOrCatchallKey> keys;
        Pattern value;
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
        final FunctionAnnotationOrUnsupportedAnnotation annotation;
        final List<Attribute> attributes;
        public LiteralExpression(Literal arg, FunctionAnnotationOrUnsupportedAnnotation annotation,
                List<Attribute> attributes) {
            this.arg = arg;
            this.annotation = annotation;
            this.attributes = attributes;
        }
    }

    static class VariableExpression implements Expression {
        final VariableRef arg;
        final FunctionAnnotationOrUnsupportedAnnotation annotation;
        final List<Attribute> attributes;
        public VariableExpression(VariableRef arg, FunctionAnnotationOrUnsupportedAnnotation annotation,
                List<Attribute> attributes) {
            this.arg = arg;
            this.annotation = annotation;
            this.attributes = attributes;
        }
    }

    interface FunctionAnnotationOrUnsupportedAnnotation {}

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
    interface Literal extends LiteralOrVariableRef {}

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

    static class FunctionAnnotation implements FunctionAnnotationOrUnsupportedAnnotation {
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

    static class UnsupportedAnnotation implements FunctionAnnotationOrUnsupportedAnnotation {
        final int sigil;
        final String source;
        public UnsupportedAnnotation(int sigil, String source) {
            this.sigil = sigil;
            this.source = source;
        }
    }

    // Markup

    static class Markup implements PatternPart {
        enum Kind {
            OPEN, CLOSE, STANDALONE;
        }
        Kind kind;
        String name;
        List<Option> options;
        List<Attribute> attributes;
    }
}
