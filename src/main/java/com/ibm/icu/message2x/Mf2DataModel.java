package com.ibm.icu.message2x;

import java.util.List;

public class Mf2DataModel {

    // Messages

    interface Message {}

    static class PatternMessage implements Message {
        List<Declaration> declarations;
        Pattern pattern;
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
        List<PatternPart> parts;
    }

    interface PatternPart {}
    
    static class StringPart implements PatternPart {       
        String value;
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
    }

    static class VariableExpression implements Expression {
        VariableRef arg;
        FunctionAnnotationOrUnsupportedAnnotation annotation;
        List<Attribute> attributes;
    }

    static class FunctionAnnotationOrUnsupportedAnnotation {
        // one of
        FunctionAnnotation functionAnnotation;
        UnsupportedAnnotation unsupportedAnnotation;
    }

    static class FunctionExpression implements Expression {
        FunctionAnnotation annotation;
        List<Attribute> attributes;
    }

    static class UnsupportedExpression {
        UnsupportedAnnotation annotation;
        List<Attribute> attributes;
    }

    static class Attribute {
        String name;
        LiteralOrVariableRef value;
    }

    // Expressions

    interface LiteralOrVariableRef {}

    static class Literal implements LiteralOrVariableRef {
        String value;
    }

    static class VariableRef implements LiteralOrVariableRef {
        String name;
    }

    static class FunctionAnnotation {
        String name;
        List<Option> options;
    }

    static class Option {
        String name;
        LiteralOrVariableRef value;
    }

    static class UnsupportedAnnotation {
        char sigil; // "!" | "%" | "^" | "&" | "*" | "+" | "<" | ">" | "?" | "~";
        String source;
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
