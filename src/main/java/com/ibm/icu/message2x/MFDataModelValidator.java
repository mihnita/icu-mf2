// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2x;

import com.ibm.icu.message2x.MFDataModel.Annotation;
import com.ibm.icu.message2x.MFDataModel.CatchallKey;
import com.ibm.icu.message2x.MFDataModel.Declaration;
import com.ibm.icu.message2x.MFDataModel.Expression;
import com.ibm.icu.message2x.MFDataModel.FunctionAnnotation;
import com.ibm.icu.message2x.MFDataModel.FunctionExpression;
import com.ibm.icu.message2x.MFDataModel.InputDeclaration;
import com.ibm.icu.message2x.MFDataModel.Literal;
import com.ibm.icu.message2x.MFDataModel.LiteralOrCatchallKey;
import com.ibm.icu.message2x.MFDataModel.LiteralOrVariableRef;
import com.ibm.icu.message2x.MFDataModel.LocalDeclaration;
import com.ibm.icu.message2x.MFDataModel.Option;
import com.ibm.icu.message2x.MFDataModel.PatternMessage;
import com.ibm.icu.message2x.MFDataModel.SelectMessage;
import com.ibm.icu.message2x.MFDataModel.VariableExpression;
import com.ibm.icu.message2x.MFDataModel.VariableRef;
import com.ibm.icu.message2x.MFDataModel.Variant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

class MFDataModelValidator {
    private final MFDataModel.Message message;

    MFDataModelValidator(MFDataModel.Message message) {
        this.message = message;
    }

    boolean validate() throws MFParseException {
        DbgUtil.spy("validate model", message);
        if (message instanceof PatternMessage) {
            validateDeclarations(((PatternMessage) message).declarations);
        } else if (message instanceof SelectMessage) {
            SelectMessage sm = (SelectMessage) message;
            validateDeclarations(sm.declarations);
            validateSelectors(sm.selectors);
            int selectorCount = sm.selectors.size();
            validateVariants(sm.variants, selectorCount);
        }
        return true;
    }

    private boolean validateVariants(List<Variant> variants, int selectorCount)
            throws MFParseException {
        if (variants == null || variants.isEmpty()) {
            error("Selection messages must have at least one variant");
        }

        // Look for an entry with all keys = '*'
        boolean hasUltimateFallback = false;
        Set<String> fakeKeys = new HashSet<>();
        for (Variant variant : variants) {
            if (variant.keys == null || variant.keys.isEmpty()) {
                error("Selection variants must have at least one key");
            }
            if (variant.keys.size() != selectorCount) {
                error("Selection variants must have the same number of variants as the selectors.");
            }
            int catchAllCount = 0;
            StringJoiner fakeKey = new StringJoiner("<<::>>");
            for (LiteralOrCatchallKey key : variant.keys) {
                if (key instanceof CatchallKey) {
                    catchAllCount++;
                    fakeKey.add("*");
                } else if (key instanceof Literal) {
                    fakeKey.add(((Literal) key).value);
                }
            }
            addWithoutDoubles(fakeKeys, fakeKey.toString(), "Dumplicate combination of keys");
            if (catchAllCount == selectorCount) {
                hasUltimateFallback = true;
            }
        }
        if (!hasUltimateFallback) {
            error("There must be one variant with all the keys being '*'");
        }
        return true;
    }

    private boolean validateSelectors(List<Expression> selectors) throws MFParseException {
        if (selectors == null || selectors.isEmpty()) {
            error("Selection messages must have selectors");
        }
        return true;
    }

    /*
     * .input {$foo :number} .input {$foo} => ERROR
     * .input {$foo :number} .local $foo={$bar} => ERROR, local foo overrides an input
     * .local $foo={...} .local $foo={...} => ERROR, foo declared twice
     * .local $a={$foo} .local $b={$foo} => NOT AN ERROR (foo is used, not declared)
     * .local $a={:f opt=$foo} .local $foo={$foo} => ERROR, foo declared after beeing used in opt
     */
    private boolean validateDeclarations(List<Declaration> declarations) throws MFParseException {
        if (declarations == null || declarations.isEmpty()) {
            return true;
        }
        Set<String> declared = new HashSet<>();
        for (Declaration declaration : declarations) {
            DbgUtil.spy("   declaration", declaration);
            if (declaration instanceof LocalDeclaration) {
                LocalDeclaration ld = (LocalDeclaration) declaration;
                addWithoutDoubles(declared, ld.name, "'" + ld.name + "' was already declared");
                validateExpression(ld.value, declared, /*is input*/ false);
            } else if (declaration instanceof InputDeclaration) {
                InputDeclaration id = (InputDeclaration) declaration;
                validateExpression(id.value, declared, /*is input*/ true);
            }
        }
        return true;
    }

    private void validateExpression(Expression expression, Set<String> declared, boolean isInput)
            throws MFParseException {
        Annotation annotation = null;
        if (expression instanceof Literal) {
            // ...{foo}... or ...{|foo|}... or ...{123}...
            // does not declare anything
        } else if (expression instanceof VariableExpression) {
            VariableExpression ve = (VariableExpression) expression;
            // ...{$foo :bar opt1=|str| opt2=$x opt3=$y}...
            // .input {$foo :number} => declares `foo`, if already declared is an error
            // .local $a={$foo} => declares `a`, but only used `foo`, does not declare it
            String argName = ve.arg.name;
            if (isInput) {
                addWithoutDoubles(declared, argName, "'" + argName + "' already declared.");
            } else {
                // Remember that we've seen it, to complain if there is a declaration later
                declared.add(ve.arg.name);
            }
            annotation = ve.annotation;
        } else if (expression instanceof FunctionExpression) {
            // ...{$foo :bar opt1=|str| opt2=$x opt3=$y}...
            FunctionExpression fe = (FunctionExpression) expression;
            annotation = fe.annotation;
        }
        if (annotation instanceof FunctionAnnotation) {
            FunctionAnnotation fa = (FunctionAnnotation) annotation;
            if (fa.options != null) {
                for (Option opt : fa.options.values()) {
                    LiteralOrVariableRef val = opt.value;
                    if (val instanceof VariableRef) {
                        // We had something like {:f option=$val}, it means we's seen `val`
                        // It is not a declaration, so not an error.
                        declared.add(((VariableRef) val).name);
                    }
                }
            }
        }
        /* One might also consider checking if the same variable is used with more than one type:
         *   .local $a = {$foo :number}
         *   .local $b = {$foo :string}
         *   .local $c = {$foo :datetime}
         *
         * But this is not necesarily an error.
         * If $foo is a number, then it might be formatter as a number, or as date (epoch time),
         * or something else.
         *
         * So it is not safe to complain. Especially with custom functions:
         *   # get the first name from a `Person` object
         *   .local $b = {$person :getField fieldName=firstName}
         *   # get formats a `Person` object
         *   .local $b = {$person :person}
         */
    }

    private boolean addWithoutDoubles(Set<String> container, String newValue, String errorMessage)
            throws MFParseException {
        if (container.contains(newValue)) {
            error(errorMessage);
            return false;
        }
        container.add(newValue);
        return true;
    }

    private void error(String text) throws MFParseException {
        DbgUtil.spy("VALIDATION FAILED", message);
        System.out.println("VALIDATION FAILED: " + message);
        throw new MFParseException(text, -1);
    }
}
