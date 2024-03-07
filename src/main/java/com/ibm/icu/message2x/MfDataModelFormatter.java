// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2x;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.ibm.icu.message2x.MfDataModel.Annotation;
import com.ibm.icu.message2x.MfDataModel.PatternPart;
import com.ibm.icu.message2x.MfDataModel.StringPart;
import com.ibm.icu.message2x.MfDataModel.VariableRef;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.CurrencyAmount;

/**
 * Takes an {@link MfDataModel} and formats it to a {@link String}
 * (and later on we will also implement formatting to a {@code FormattedMessage}).
 */
// TODO: move this in the MessageFormatter
class MfDataModelFormatter {
    private final Locale locale;
    private final MfDataModel.Message dm;

    final MfFunctionRegistry standardFunctions;
    final MfFunctionRegistry customFunctions;
    private static final MfFunctionRegistry EMPTY_REGISTY = MfFunctionRegistry.builder().build();

    MfDataModelFormatter(MfDataModel.Message dm, Locale locale, MfFunctionRegistry customFunctionRegistry) {
        this.locale = locale;
        this.dm = dm;
        this.customFunctions = customFunctionRegistry == null ? EMPTY_REGISTY : customFunctionRegistry;

        standardFunctions = MfFunctionRegistry.builder()
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
        MfDataModel.Pattern patternToRender = null;
        List<MfDataModel.Declaration> declarations = null;

        if (dm instanceof MfDataModel.PatternMessage) {
            MfDataModel.PatternMessage pm = (MfDataModel.PatternMessage) dm;
            patternToRender = pm.pattern;
            declarations = pm.declarations;
        } else if (dm instanceof MfDataModel.SelectMessage) {
            MfDataModel.SelectMessage sm = (MfDataModel.SelectMessage) dm;
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
        for (MfDataModel.PatternPart part : patternToRender.parts) {
            System.out.println(part);
            if (part instanceof MfDataModel.StringPart) {
                MfDataModel.StringPart sPart = (StringPart) part;
                result.append(sPart.value);
            } else if (part instanceof MfDataModel.VariableExpression) {
                MfDataModel.VariableExpression varPart = (MfDataModel.VariableExpression) part;
                Annotation annot = varPart.annotation; // function name
                VariableRef arg = varPart.arg; // argument
//                FormattedPlaceholder fp = formatPlaceholder((Expression) part, arguments, false);
//                result.append(fp.toString());
            } else {
                throw new IllegalArgumentException("Unknown part type: " + part);
            }
        }
        return result.toString();
    }
}
