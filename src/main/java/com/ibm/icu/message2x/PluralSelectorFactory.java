// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2x;

import java.util.Locale;
import java.util.Map;

import com.ibm.icu.number.FormattedNumber;
import com.ibm.icu.text.FormattedValue;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.PluralType;

/**
 * Creates a {@link Selector} doing plural selection, similar to <code>{exp, plural}</code>
 * in {@link com.ibm.icu.text.MessageFormat}.
 */
class PluralSelectorFactory implements SelectorFactory {
    /**
     * Creates a {@code PluralSelectorFactory} of the desired type.
     */
    PluralSelectorFactory() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Selector createSelector(Locale locale, Map<String, Object> fixedOptions) {
        String type = OptUtils.getString(fixedOptions, "select", "");
        PluralType pluralType;
        switch (type) {
            case "ordinal":
                pluralType = PluralType.ORDINAL;
                break;
            case "cardinal": // intentional fallthrough
            default:
                pluralType = PluralType.CARDINAL;
        }

        PluralRules rules = PluralRules.forLocale(locale, pluralType);
        return new PluralSelectorImpl(rules, fixedOptions);
    }

    private static class PluralSelectorImpl implements Selector {
        private final PluralRules rules;
        private Map<String, Object> fixedOptions;

        private PluralSelectorImpl(PluralRules rules, Map<String, Object> fixedOptions) {
            this.rules = rules;
            this.fixedOptions = fixedOptions;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean matches(Object value, String key, Map<String, Object> variableOptions) {
            if (value == null) {
                return false;
            }
            if ("*".equals(key)) {
                return true;
            }

            Integer offset = OptUtils.getInteger(variableOptions, "offset");
            if (offset == null && fixedOptions != null) {
                offset = OptUtils.getInteger(fixedOptions, "offset");
            }
            if (offset == null) {
                offset = 0;
            }

            double valToCheck = Double.MIN_VALUE;
            FormattedValue formattedValToCheck = null;
            if (value instanceof FormattedPlaceholder) {
                FormattedPlaceholder fph = (FormattedPlaceholder) value;
                value = fph.getInput();
                formattedValToCheck = fph.getFormattedValue();
            }

            if (value instanceof Double) {
                valToCheck = (double) value;
            } else if (value instanceof Integer) {
                valToCheck = (Integer) value;
            } else {
                return false;
            }

            // If there is nothing "tricky" about the formatter part we compare values directly.
            // Right now ICU4J checks if the formatter is a DecimalFormt, which also feels "hacky".
            // We need something better.

            Integer vo = OptUtils.getInteger(variableOptions, "minimumFractionDigits");
            Integer fo = OptUtils.getInteger(fixedOptions, "minimumFractionDigits");
            if (vo == null || fo == null) {
                try { // for match exact.
                    if (Double.parseDouble(key) == valToCheck) {
                        return true;
                    }
                } catch (NumberFormatException e) {
                }
            }

            String match = formattedValToCheck instanceof FormattedNumber
                    ? rules.select((FormattedNumber) formattedValToCheck)
                    : rules.select(valToCheck - offset);
            return match.equals(key);
        }
    }
}
