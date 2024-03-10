// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2x;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This maps closely to the official specification.
 * Since it is not final, we will not add javadoc everywhere.
 *
 * <p>See <a target="github" href="https://github.com/unicode-org/message-format-wg/blob/main/spec/data-model/README.md">the
 * latest description</a>.</p>
 *
 * @internal ICU 72 technology preview
 * TzuAt deprecated This API is for technology preview only.
 */
// TZUAT Deprecated
@SuppressWarnings("javadoc")
public class MFDataModel {

    private MFDataModel() {
        // Prevent instantiation
    }

    // Messages

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    public interface Message {
        // Provides a common type for PatternMessage and SelectMessage.
    }

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    static public class PatternMessage implements Message {
        final List<Declaration> declarations;
        final Pattern pattern;

        /**
         * @internal ICU 72 technology preview
         * TzuAt deprecated This API is for technology preview only.
         */
        // TZUAT Deprecated
        public PatternMessage(List<Declaration> declarations, Pattern pattern) {
            this.declarations = declarations;
            this.pattern = pattern;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    static public class SelectMessage implements Message {
        final List<Declaration> declarations;
        final List<Expression> selectors;
        final List<Variant> variants;

        /**
         * @internal ICU 72 technology preview
         * TzuAt deprecated This API is for technology preview only.
         */
        // TZUAT Deprecated
        public SelectMessage(List<Declaration> declarations, List<Expression> selectors, List<Variant> variants) {
            this.declarations = declarations;
            this.selectors = selectors;
            this.variants = variants;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    public interface Declaration {
        // Provides a common type for InputDeclaration, LocalDeclaration, and UnsupportedStatement.
    }

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    static public class InputDeclaration implements Declaration {
        final String name;
        final VariableExpression value;

        /**
         * @internal ICU 72 technology preview
         * TzuAt deprecated This API is for technology preview only.
         */
        // TZUAT Deprecated
        public InputDeclaration(String name, VariableExpression value) {
            this.name = name;
            this.value = value;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    static public class LocalDeclaration implements Declaration {
        final String name;
        final Expression value;

        /**
         * @internal ICU 72 technology preview
         * TzuAt deprecated This API is for technology preview only.
         */
        // TZUAT Deprecated
        public LocalDeclaration(String name, Expression value) {
            this.name = name;
            this.value = value;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    static public class UnsupportedStatement implements Declaration {
        final String keyword;
        final String body;
        final List<Expression> expressions;

        /**
         * @internal ICU 72 technology preview
         * TzuAt deprecated This API is for technology preview only.
         */
        // TZUAT Deprecated
        public UnsupportedStatement(String keyword, String body, List<Expression> expressions) {
            this.keyword = keyword;
            this.body = body;
            this.expressions = expressions;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    public interface LiteralOrCatchallKey {
        // Provides a common type for the selection keys: Variant, Literal, or CatchallKey.
    }

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    static public class Variant implements LiteralOrCatchallKey {
        final List<LiteralOrCatchallKey> keys;
        final Pattern value;

        /**
         * @internal ICU 72 technology preview
         * TzuAt deprecated This API is for technology preview only.
         */
        // TZUAT Deprecated
        public Variant(List<LiteralOrCatchallKey> keys, Pattern value) {
            this.keys = keys;
            this.value = value;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    static public class CatchallKey implements LiteralOrCatchallKey {
        // String value; // Always '*' in MF2
    }

    // Patterns

    // type Pattern = Array<string | Expression | Markup>;
    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    static public class Pattern {
        final List<PatternPart> parts;

        Pattern() {
            this.parts = new ArrayList<>();
        }
    }

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    public interface PatternPart {
        // Provides a common type for StringPart and Expression.
    }

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    static public class StringPart implements PatternPart {
        final String value;

        StringPart(String value) {
            this.value = value;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    public interface Expression extends PatternPart {
        // Provides a common type for all kind of expressions:
        // LiteralExpression, VariableExpression, FunctionExpression, UnsupportedExpression, Markup
    }

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    static public class LiteralExpression implements Expression {
        final Literal arg;
        final Annotation annotation;
        final List<Attribute> attributes;

        /**
         * @internal ICU 72 technology preview
         * TzuAt deprecated This API is for technology preview only.
         */
        // TZUAT Deprecated
        public LiteralExpression(Literal arg, Annotation annotation, List<Attribute> attributes) {
            this.arg = arg;
            this.annotation = annotation;
            this.attributes = attributes;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    static public class VariableExpression implements Expression {
        final VariableRef arg;
        final Annotation annotation;
        final List<Attribute> attributes;

        /**
         * @internal ICU 72 technology preview
         * TzuAt deprecated This API is for technology preview only.
         */
        // TZUAT Deprecated
        public VariableExpression(VariableRef arg, Annotation annotation, List<Attribute> attributes) {
            this.arg = arg;
            this.annotation = annotation;
            this.attributes = attributes;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    public interface Annotation {
        // Provides a common type for FunctionAnnotation, UnsupportedAnnotation
    }

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    static public class FunctionExpression implements Expression {
        final FunctionAnnotation annotation;
        final List<Attribute> attributes;

        /**
         * @internal ICU 72 technology preview
         * TzuAt deprecated This API is for technology preview only.
         */
        // TZUAT Deprecated
        public FunctionExpression(FunctionAnnotation annotation, List<Attribute> attributes) {
            this.annotation = annotation;
            this.attributes = attributes;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    static public class UnsupportedExpression implements Expression {
        final UnsupportedAnnotation annotation;
        final List<Attribute> attributes;

        /**
         * @internal ICU 72 technology preview
         * TzuAt deprecated This API is for technology preview only.
         */
        // TZUAT Deprecated
        public UnsupportedExpression(UnsupportedAnnotation annotation, List<Attribute> attributes) {
            this.annotation = annotation;
            this.attributes = attributes;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    static public class Attribute {
        final String name;
        final LiteralOrVariableRef value;

        /**
         * @internal ICU 72 technology preview
         * TzuAt deprecated This API is for technology preview only.
         */
        // TZUAT Deprecated
        public Attribute(String name, LiteralOrVariableRef value) {
            this.name = name;
            this.value = value;
        }
    }

    // Expressions

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    public interface LiteralOrVariableRef {
        // Provides a common type for Literal and VariableRef,
        // to represent things like `foo` / `|foo|` / `1234` (literals)
        // and `$foo` (VariableRef), as argument for placeholders or value in options.
    }

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    static public class Literal implements LiteralOrVariableRef, LiteralOrCatchallKey {
        final String value;

        /**
         * @internal ICU 72 technology preview
         * TzuAt deprecated This API is for technology preview only.
         */
        // TZUAT Deprecated
        public Literal(String value) {
            this.value = value;
        }
    }


    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    static public class VariableRef implements LiteralOrVariableRef {
        final String name;

        /**
         * @internal ICU 72 technology preview
         * TzuAt deprecated This API is for technology preview only.
         */
        // TZUAT Deprecated
        public VariableRef(String name) {
            this.name = name;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    static public class FunctionAnnotation implements Annotation {
        final String name;
        final Map<String, Option> options;

        /**
         * @internal ICU 72 technology preview
         * TzuAt deprecated This API is for technology preview only.
         */
        // TZUAT Deprecated
        public FunctionAnnotation(String name, Map<String, Option> options) {
            this.name = name;
            this.options = options;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    static public class Option {
        final String name;
        final LiteralOrVariableRef value;

        /**
         * @internal ICU 72 technology preview
         * TzuAt deprecated This API is for technology preview only.
         */
        // TZUAT Deprecated
        public Option(String name, LiteralOrVariableRef value) {
            this.name = name;
            this.value = value;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    static public class UnsupportedAnnotation implements Annotation {
        final String source;

        /**
         * @internal ICU 72 technology preview
         * TzuAt deprecated This API is for technology preview only.
         */
        // TZUAT Deprecated
        public UnsupportedAnnotation(String source) {
            this.source = source;
        }
    }

    // Markup

    /**
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    static public class Markup implements Expression {
        enum Kind {
            OPEN, CLOSE, STANDALONE
        }

        final Kind kind;
        final String name;
        final Map<String, Option> options;
        final List<Attribute> attributes;

        /**
         * @internal ICU 72 technology preview
         * TzuAt deprecated This API is for technology preview only.
         */
        // TZUAT Deprecated
        public Markup(Kind kind, String name, Map<String, Option> options, List<Attribute> attributes) {
            this.kind = kind;
            this.name = name;
            this.options = options;
            this.attributes = attributes;
        }
    }
}
