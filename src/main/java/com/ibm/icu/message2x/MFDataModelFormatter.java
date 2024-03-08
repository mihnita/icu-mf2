// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2x;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.ibm.icu.message2x.MFDataModel.Annotation;
import com.ibm.icu.message2x.MFDataModel.FunctionAnnotation;
import com.ibm.icu.message2x.MFDataModel.FunctionExpression;
import com.ibm.icu.message2x.MFDataModel.Literal;
import com.ibm.icu.message2x.MFDataModel.LiteralExpression;
import com.ibm.icu.message2x.MFDataModel.LiteralOrVariableRef;
import com.ibm.icu.message2x.MFDataModel.Option;
import com.ibm.icu.message2x.MFDataModel.StringPart;
import com.ibm.icu.message2x.MFDataModel.UnsupportedExpression;
import com.ibm.icu.message2x.MFDataModel.VariableRef;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.CurrencyAmount;

/**
 * Takes an {@link MFDataModel} and formats it to a {@link String}
 * (and later on we will also implement formatting to a {@code FormattedMessage}).
 */
// TODO: move this in the MessageFormatter
class MFDataModelFormatter {
    private final Locale locale;
    private final MFDataModel.Message dm;

    final MFFunctionRegistry standardFunctions;
    final MFFunctionRegistry customFunctions;
    private static final MFFunctionRegistry EMPTY_REGISTY = MFFunctionRegistry.builder().build();

    MFDataModelFormatter(MFDataModel.Message dm, Locale locale, MFFunctionRegistry customFunctionRegistry) {
        this.locale = locale;
        this.dm = dm;
        this.customFunctions = customFunctionRegistry == null ? EMPTY_REGISTY : customFunctionRegistry;

        standardFunctions = MFFunctionRegistry.builder()
                // Date/time formatting
                .setFormatter("datetime", new DateTimeFormatterFactory())
                .setDefaultFormatterNameForType(Date.class, "datetime")
                .setDefaultFormatterNameForType(Calendar.class, "datetime")

                // Number formatting
                .setFormatter("number", new NumberFormatterFactory())
                .setDefaultFormatterNameForType(Integer.class, "number")
                .setDefaultFormatterNameForType(Double.class, "number")
                .setDefaultFormatterNameForType(Number.class, "number")
                .setDefaultFormatterNameForType(CurrencyAmount.class, "number")

                // Format that returns "to string"
                .setFormatter("identity", new IdentityFormatterFactory())
                .setDefaultFormatterNameForType(String.class, "identity")
                .setDefaultFormatterNameForType(CharSequence.class, "identity")

                // Register the standard selectors
                .setSelector("plural", new PluralSelectorFactory("cardinal"))
                .setSelector("selectordinal", new PluralSelectorFactory("ordinal"))
                .setSelector("select", new TextSelectorFactory())
                .setSelector("gender", new TextSelectorFactory())

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
//            findBestMatchingPattern(selectors, arguments);
//            sm.declarations;
//            sm.selectors;
//            sm.variants
        }

        if (patternToRender == null) {
            return "ERROR!";
        }

//        List<Expression> selectors = dm.getSelectors();
//        Pattern patternToRender = selectors.isEmpty()
//                ? dm.getPattern()
//                : findBestMatchingPattern(selectors, arguments);
//
        StringBuilder result = new StringBuilder();
        for (MFDataModel.PatternPart part : patternToRender.parts) {
            System.out.println(part);

            Annotation annotation = null; // function name
            Object toFormat = null;

            if (part instanceof MFDataModel.StringPart) {
                MFDataModel.StringPart sPart = (StringPart) part;
                result.append(sPart.value);
            } else if (part instanceof MFDataModel.VariableExpression) {
                MFDataModel.VariableExpression varPart = (MFDataModel.VariableExpression) part;
                annotation = varPart.annotation; // function name
                VariableRef argument = varPart.arg; // argument
                toFormat = argument.name;
//                FormattedPlaceholder fp = formatPlaceholder((Expression) part, arguments, false);
//                result.append(fp.toString());
            } else if (part instanceof MFDataModel.FunctionExpression) { // Function without arguments
                MFDataModel.FunctionExpression fe = (FunctionExpression) part;
                annotation = fe.annotation;
            } else if (part instanceof MFDataModel.LiteralExpression) {
                MFDataModel.LiteralExpression le = (LiteralExpression) part;
                annotation = le.annotation;
                Literal argument = le.arg;
                toFormat = argument.value;
            } else if (part instanceof MFDataModel.Markup) {
            } else if (part instanceof MFDataModel.UnsupportedExpression) {
                MFDataModel.UnsupportedExpression ue = (UnsupportedExpression) part;
                formattingError("We don't know how to format UnsupportedExpression expressions (" + ue.annotation + ")");
            } else {
                formattingError("Unknown part type: " + part);
            }
            
            if (annotation instanceof FunctionAnnotation) {
                FunctionAnnotation fa = (FunctionAnnotation) annotation;
                String functionName = fa.name;
                List<Option> options = fa.options;
                for (Option option : options) {
                    String name = option.name;
                    LiteralOrVariableRef value = option.value;
                }
                FormatterFactory funcFactory = getFormattingFunctionFactoryByName(toFormat, functionName);
                Formatter ff = funcFactory.createFormatter(locale, new HashMap<String, Object>());
                String res = ff.formatToString(toFormat, arguments);
                result.append(res);
            }
        }
        return result.toString();
    }

    private void formattingError(String message) {
        throw new IllegalArgumentException(message);
    }
    
    FormatterFactory getFormattingFunctionFactoryByName(Object toFormat, String functionName) {
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
}
