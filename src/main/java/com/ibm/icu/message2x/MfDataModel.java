package com.ibm.icu.message2x;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class MfDataModel {

    // Messages

    interface Message {}

    static class PatternMessage implements Message {
        final List<Declaration> declarations;
        final Pattern pattern;

        public PatternMessage(List<Declaration> declarations, Pattern pattern) {
            super();
            this.declarations = declarations;
            this.pattern = pattern;
        }

        @Override
        public String toString() {
            return "PatternMessage ["
                    + "declarations=" + declarations
                    + ", pattern=" + pattern
                    + "]";
        }
    }

    static class SelectMessage implements Message {
        List<Declaration> declarations;
        List<Expression> selectors;
        List<Variant> variants;
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
        @Override
        public String toString() {
            StringJoiner result = new StringJoiner(", ", "[", "]");
            for (PatternPart part: parts) {
                result.add(part.toString());
            }
            return result.toString();
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

        @Override
        public String toString() {
            return "StringPart ["
                    + "value=" + stringEscape(value)
                    + "]";
        }
    }
    
    private static String stringEscape(String text) {
        final StringBuilder result = new StringBuilder();
        result.append("\"");
        text.chars().forEach(ch ->{
            switch (ch) {
                case '\n': result.append("\\n"); break;
                case '\t': result.append("\\t"); break;
                case '\r': result.append("\\r"); break;
                case '\b': result.append("\\b"); break;
                case '\f': result.append("\\f"); break;
                default: result.append((char)ch);
            }
        });
        result.append("\"");
        return result.toString();
    }

    // type Expression =
    //         | LiteralExpression
    //         | VariableExpression
    //         | FunctionExpression
    //         | UnsupportedExpression
    interface Expression extends PatternPart {}

    static class LiteralExpression implements Expression {
        Literal arg;
        FunctionAnnotationOrUnsupportedAnnotation annotation;
        List<Attribute> attributes;

        @Override
        public String toString() {
            return "LiteralExpression ["
                    + "arg=" + arg
                    + ", annotation=" + annotation
                    + ", attributes=" + attributes
                    + "]";
        }
    }

    static class VariableExpression implements Expression {
        VariableRef arg;
        FunctionAnnotationOrUnsupportedAnnotation annotation;
        List<Attribute> attributes;
        @Override
        public String toString() {
            return "VariableExpression ["
                    + "arg=" + arg
                    + ", annotation=" + annotation
                    + ", attributes=" + attributes
                    + "]";
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

        @Override
        public String toString() {
            return "FunctionExpression ["
                    + "annotation=" + annotation
                    + ", attributes=" + attributes
                    + "]";
        }
    }

    static class UnsupportedExpression implements Expression {
        final UnsupportedAnnotation annotation;
        final List<Attribute> attributes;

        public UnsupportedExpression(UnsupportedAnnotation annotation, List<Attribute> attributes) {
            this.annotation = annotation;
            this.attributes = attributes;
        }
        @Override
        public String toString() {
            return "UnsupportedExpression [annotation=" + annotation + ", attributes=" + attributes + "]";
        }
    }

    static class Attribute {
        String name;
        LiteralOrVariableRef value;
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
        @Override
        public String toString() {
            return "Literal [value=" + value + "]";
        }
    }

    // Not in the official data model
    static class NumberLiteral implements Literal {
        final Number value;
        public NumberLiteral(Number value) {
            this.value = value;
        }
        @Override
        public String toString() {
            return "NumberLiteral [value=" + value + "]";
        }
    }

    static class VariableRef implements LiteralOrVariableRef {
        final String name;
        public VariableRef(String name) {
            this.name = name;
        }
        @Override
        public String toString() {
            return "VariableRef [name=" + name + "]";
        }
    }

    static class FunctionAnnotation implements FunctionAnnotationOrUnsupportedAnnotation {
        final String name;
        final List<Option> options;

        public FunctionAnnotation(String name, List<Option> options) {
            this.name = name;
            this.options = options;
        }

        @Override
        public String toString() {
            return "FunctionAnnotation ["
                    + "name=" + name
                    + ", options=" + options
                    + "]";
        }
    }

    static class Option {
        final String name;
        final LiteralOrVariableRef value;
        public Option(String name, LiteralOrVariableRef value) {
            super();
            this.name = name;
            this.value = value;
        }
        @Override
        public String toString() {
            return "Option [name=" + name + ", value=" + value + "]";
        }
    }

    static class UnsupportedAnnotation implements FunctionAnnotationOrUnsupportedAnnotation {
        final int sigil;
        final String source;
        public UnsupportedAnnotation(int sigil, String source) {
            super();
            this.sigil = sigil;
            this.source = source;
        }
        @Override
        public String toString() {
            return "UnsupportedAnnotation [sigil=" + sigil + ", source=" + source + "]";
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
