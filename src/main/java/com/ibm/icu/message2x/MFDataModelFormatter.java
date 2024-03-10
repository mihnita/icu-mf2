// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2x;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.ibm.icu.message2x.MFDataModel.Annotation;
import com.ibm.icu.message2x.MFDataModel.Declaration;
import com.ibm.icu.message2x.MFDataModel.Expression;
import com.ibm.icu.message2x.MFDataModel.FunctionAnnotation;
import com.ibm.icu.message2x.MFDataModel.FunctionExpression;
import com.ibm.icu.message2x.MFDataModel.InputDeclaration;
import com.ibm.icu.message2x.MFDataModel.Literal;
import com.ibm.icu.message2x.MFDataModel.LiteralExpression;
import com.ibm.icu.message2x.MFDataModel.LiteralOrVariableRef;
import com.ibm.icu.message2x.MFDataModel.LocalDeclaration;
import com.ibm.icu.message2x.MFDataModel.Option;
import com.ibm.icu.message2x.MFDataModel.StringPart;
import com.ibm.icu.message2x.MFDataModel.VariableRef;
import com.ibm.icu.text.FormattedValue;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.CurrencyAmount;

/**
 * Takes an {@link MFDataModel} and formats it to a {@link String}
 * (and later on we will also implement formatting to a {@code FormattedMessage}).
 */
// TODO: move this in the MessageFormatter?
class MFDataModelFormatter {
    private final Locale locale;
    private final MFDataModel.Message dm;

    private final MFFunctionRegistry standardFunctions;
    private final MFFunctionRegistry customFunctions;
    private static final MFFunctionRegistry EMPTY_REGISTY = MFFunctionRegistry.builder().build();

    MFDataModelFormatter(MFDataModel.Message dm, Locale locale, MFFunctionRegistry customFunctionRegistry) {
        this.locale = locale;
        this.dm = dm;
        this.customFunctions = customFunctionRegistry == null ? EMPTY_REGISTY : customFunctionRegistry;

        standardFunctions = MFFunctionRegistry.builder()
                // Date/time formatting
                // TODO: `:date`, `:time` ?
                .setFormatter("datetime", new DateTimeFormatterFactory())
                .setDefaultFormatterNameForType(Date.class, "datetime")
                .setDefaultFormatterNameForType(Calendar.class, "datetime")
                .setDefaultFormatterNameForType(java.util.Calendar.class, "datetime")

                // Number formatting
                // TODO: `:integer` ?
                .setFormatter("number", new NumberFormatterFactory())
                .setDefaultFormatterNameForType(Integer.class, "number")
                .setDefaultFormatterNameForType(Double.class, "number")
                .setDefaultFormatterNameForType(Number.class, "number")
                .setDefaultFormatterNameForType(CurrencyAmount.class, "number")

                // Format that returns "to string"
                .setFormatter("string", new IdentityFormatterFactory())
                .setDefaultFormatterNameForType(String.class, "string")
                .setDefaultFormatterNameForType(CharSequence.class, "string")

                // Register the standard selectors
                // TODO: update this to spec
                .setSelector("number", new PluralSelectorFactory("cardinal"))
//                .setSelector("selectordinal", new PluralSelectorFactory("ordinal"))
                .setSelector("string", new PluralSelectorFactory("ordinal"))

                .build();
    }

    String format(Map<String, Object> arguments) {
        MFDataModel.Pattern patternToRender = null;
        List<MFDataModel.Declaration> declarations = null;

        if (dm instanceof MFDataModel.PatternMessage) {
            MFDataModel.PatternMessage pm = (MFDataModel.PatternMessage) dm;
            patternToRender = pm.pattern;
            declarations = pm.declarations;
        } else if (dm instanceof MFDataModel.SelectMessage) {
            MFDataModel.SelectMessage sm = (MFDataModel.SelectMessage) dm;
            declarations = sm.declarations;
//            findBestMatchingPattern(selectors, arguments);
//            sm.declarations;
//            sm.selectors;
//            sm.variants
        }

        if (patternToRender == null) {
            return "ERROR!";
        }

        Map<String, Object> variables = resolveDeclarations(declarations, arguments);

        StringBuilder result = new StringBuilder();
        for (MFDataModel.PatternPart part : patternToRender.parts) {
            if (part instanceof MFDataModel.StringPart) {
                MFDataModel.StringPart sPart = (StringPart) part;
                result.append(sPart.value);
            } else if (part instanceof MFDataModel.Expression) {
                FormattedPlaceholder formattedExpression = formatExpression((Expression) part, variables, arguments);
                result.append(formattedExpression.getFormattedValue().toString());
            } else if (part instanceof MFDataModel.Markup) {
                // Ignore
            } else if (part instanceof MFDataModel.UnsupportedExpression) {
                // Ignore
            } else {
                formattingError("Unknown part type: " + part);
            }
        }
        return result.toString();
    }

    static private void formattingError(String message) {
        throw new IllegalArgumentException(message);
    }

    private FormatterFactory getFormattingFunctionFactoryByName(Object toFormat, String functionName) {
        // Get a function name from the type of the object to format
        if (functionName == null || functionName.isEmpty()) {
            if (toFormat == null) {
                // The object to format is null, and no function provided.
                return null;
            }
            Class<?> clazz = toFormat.getClass();
            functionName = standardFunctions.getDefaultFormatterNameForType(clazz);
            if (functionName == null) {
                functionName = customFunctions.getDefaultFormatterNameForType(clazz);
            }
            if (functionName == null) {
                throw new IllegalArgumentException("Object to format without a function, and unknown type: "
                        + toFormat.getClass().getName());
            }
        }

        FormatterFactory func = standardFunctions.getFormatter(functionName);
        if (func == null) {
            func = customFunctions.getFormatter(functionName);
            if (func == null) {
                throw new IllegalArgumentException("Can't find an implementation for function: '"
                        + functionName + "'");
            }
        }
        return func;
    }

    private static Object resolveLiteralOrVariable(LiteralOrVariableRef value, Map<String, Object> localVars, Map<String, Object> arguments) {
        if (value instanceof Literal) {
            String val = ((Literal) value).value;
            Number nr = tryParsingAsNumber(val);
            if (nr != null) {
                return nr;
            }
            return val;
        } else if (value instanceof VariableRef) {
            String varName = ((VariableRef) value).name;
            Object val = localVars.get(varName);
            if (val == null) {
                val = localVars.get(varName);
            }
            if (val == null) {
                val = arguments.get(varName);
            }
            return val;
        }
        return value;
    }

    private static Number tryParsingAsNumber(String text) {
        Number result = null;
        try {
            result = Long.parseLong(text);
        } catch (NumberFormatException e) {}
        try {
            result = Double.parseDouble(text);
        } catch (NumberFormatException e) {}
        try {
            result = new BigDecimal(text);
        } catch (NumberFormatException e) {}
        return result;
    }

    private static Map<String, Object> convertOptions(Map<String, Option> options, Map<String, Object> localVars, Map<String, Object> arguments) {
        Map<String, Object> result = new HashMap<>();
        for (Option option : options.values()) {
            result.put(option.name, resolveLiteralOrVariable(option.value, localVars, arguments));
        }
        return result;
    }

    
    /**
     * @param expression the expression to forma
     * @param variables local variables, created from declarations (`.input` and `.local`)
     * @param arguments the arguments passed at runtime to be formatted (`mf.format(arguments)`)
     */
    private FormattedPlaceholder formatExpression(Expression expression,
            Map<String, Object> variables, Map<String, Object> arguments) {

        Annotation annotation = null; // function name
        String functionName = null;
        Object toFormat = null;
        Map<String, Object> options = new HashMap<>();

        if (expression instanceof MFDataModel.VariableExpression) {
            MFDataModel.VariableExpression varPart = (MFDataModel.VariableExpression) expression;
            annotation = varPart.annotation; // function name & options
            Object resolved = resolveLiteralOrVariable(varPart.arg, variables, arguments);
            if (resolved instanceof FormattedPlaceholder) {
                Object input = ((FormattedPlaceholder) resolved).getInput();
                if (input instanceof ResolvedExpression) {
                    ResolvedExpression re = (ResolvedExpression) input;
                    toFormat = re.argument;
                    functionName = re.functionName;
                    options.putAll(re.options);
                } else {
                    toFormat = input;
                }
            } else {
                toFormat = resolved;
            }
        } else if (expression instanceof MFDataModel.FunctionExpression) { // Function without arguments
            MFDataModel.FunctionExpression fe = (FunctionExpression) expression;
            annotation = fe.annotation;
        } else if (expression instanceof MFDataModel.LiteralExpression) {
            MFDataModel.LiteralExpression le = (LiteralExpression) expression;
            annotation = le.annotation;
            toFormat = resolveLiteralOrVariable(le.arg, variables, arguments);
        } else {
            return new FormattedPlaceholder(expression, new PlainStringFormattedValue("{\uFFFD}"));
        }

        if (annotation instanceof FunctionAnnotation) {
            FunctionAnnotation fa = (FunctionAnnotation) annotation;
            if (functionName != null && !functionName.equals(fa.name)) {
                    formattingError("invalid function overrides, '" + functionName + "' <> '" + fa.name + "'");
            }
            functionName = fa.name;
            Map<String, Object> newOptions = convertOptions(fa.options, variables, arguments);
            options.putAll(newOptions);
        }

        FormatterFactory funcFactory = getFormattingFunctionFactoryByName(toFormat, functionName);
        Formatter ff = funcFactory.createFormatter(locale, options);
        String res = ff.formatToString(toFormat, arguments);

        ResolvedExpression resExpression = new ResolvedExpression(toFormat, functionName, options);
        return new FormattedPlaceholder(resExpression, new PlainStringFormattedValue(res));
    }

    static class ResolvedExpression implements Expression {
        final Object argument;
        final String functionName;
        final Map<String, Object> options;

        public ResolvedExpression(Object argument, String functionName, Map<String, Object> options) {
            this.argument = argument;
            this.functionName = functionName;
            this.options = options;
        }
    }

    private Map<String, Object> resolveDeclarations(List<MFDataModel.Declaration> declarations, Map<String, Object> arguments) {
        Map<String, Object> variables = new HashMap<>();
        String name;
        Expression value;
        if (declarations != null) {
            for (Declaration declaration : declarations) {
                if (declaration instanceof InputDeclaration) {
                    name = ((InputDeclaration) declaration).name;
                    value = ((InputDeclaration) declaration).value;
                } else if (declaration instanceof LocalDeclaration) {
                    name = ((LocalDeclaration) declaration).name;
                    value = ((LocalDeclaration) declaration).value;
                } else {
                    continue; 
                }
                FormattedPlaceholder fmt = formatExpression(value, variables, arguments);
                variables.put(name, fmt);
            }
        }
        return variables;
    }
}
