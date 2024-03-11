// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2x;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.number.FormattedNumber;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.NumberFormatter.GroupingStrategy;
import com.ibm.icu.number.NumberFormatter.SignDisplay;
import com.ibm.icu.number.Precision;
import com.ibm.icu.number.UnlocalizedNumberFormatter;
import com.ibm.icu.text.FormattedValue;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.PluralType;
import com.ibm.icu.util.CurrencyAmount;


/**
 * Creates a {@link Formatter} doing numeric formatting, similar to <code>{exp, number}</code>
 * in {@link com.ibm.icu.text.MessageFormat}.
 */
class NumberFormatterFactory implements FormatterFactory, SelectorFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public Formatter createFormatter(Locale locale, Map<String, Object> fixedOptions) {
        return new NumberFormatterImpl(locale, fixedOptions);
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
        return new PluralSelectorImpl(locale, rules, fixedOptions);
    }

    static class NumberFormatterImpl implements Formatter {
        private final Locale locale;
        private final Map<String, Object> fixedOptions;
        private final LocalizedNumberFormatter icuFormatter;
        final boolean advanced;

        NumberFormatterImpl(Locale locale, Map<String, Object> fixedOptions) {
            this.locale = locale;
            this.fixedOptions = new HashMap<>(fixedOptions);
            String skeleton = OptUtils.getString(fixedOptions, "skeleton");
            boolean fancy = skeleton != null;
            this.icuFormatter = formatterForOptions(locale, fixedOptions);
            this.advanced = fancy;
        }

        LocalizedNumberFormatter getIcuFormatter() {
            return icuFormatter;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String formatToString(Object toFormat, Map<String, Object> variableOptions) {
            return format(toFormat, variableOptions).toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public FormattedPlaceholder format(Object toFormat, Map<String, Object> variableOptions) {
            LocalizedNumberFormatter realFormatter;
            if (variableOptions.isEmpty()) {
                realFormatter = this.icuFormatter;
            } else {
                Map<String, Object> mergedOptions = new HashMap<>(fixedOptions);
                mergedOptions.putAll(variableOptions);
                // This is really wasteful, as we don't use the existing
                // formatter if even one option is variable.
                // We can optimize, but for now will have to do.
                realFormatter = formatterForOptions(locale, mergedOptions);
            }

            Integer offset = OptUtils.getInteger(variableOptions, "icu:offset");
            if (offset == null && fixedOptions != null) {
                offset = OptUtils.getInteger(fixedOptions, "icu:offset");
            }
            if (offset == null) {
                offset = 0;
            }

            FormattedValue result = null;
            if (toFormat == null) {
                // This is also what MessageFormat does.
                throw new NullPointerException("Argument to format can't be null");
            } else if (toFormat instanceof Double) {
                result = realFormatter.format((double) toFormat - offset);
            } else if (toFormat instanceof Long) {
                result = realFormatter.format((long) toFormat - offset);
            } else if (toFormat instanceof Integer) {
                result = realFormatter.format((int) toFormat - offset);
            } else if (toFormat instanceof BigDecimal) {
                BigDecimal bd = (BigDecimal) toFormat;
                result = realFormatter.format(bd.subtract(BigDecimal.valueOf(offset)));
            } else if (toFormat instanceof Number) {
                result = realFormatter.format(((Number) toFormat).doubleValue() - offset);
            } else if (toFormat instanceof CurrencyAmount) {
                result = realFormatter.format((CurrencyAmount) toFormat);
            } else {
                // The behavior is not in the spec, will be in the registry.
                // We can return "NaN", or try to parse the string as a number
                String strValue = Objects.toString(toFormat);
                Number nrValue = OptUtils.asNumber(strValue);
                if (nrValue != null) {
                    result = realFormatter.format(nrValue.doubleValue() - offset);
                } else {
                    result = new PlainStringFormattedValue("NaN");
                }
            }
            return new FormattedPlaceholder(toFormat, result);
        }
    }

    private static class PluralSelectorImpl implements Selector {
        private final PluralRules rules;
        private Map<String, Object> fixedOptions;
        private LocalizedNumberFormatter icuFormatter;

        private PluralSelectorImpl(Locale locale, PluralRules rules, Map<String, Object> fixedOptions) {
            this.rules = rules;
            this.fixedOptions = fixedOptions;
            this.icuFormatter = formatterForOptions(locale, fixedOptions);
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
            if (value instanceof FormattedPlaceholder) {
                FormattedPlaceholder fph = (FormattedPlaceholder) value;
                value = fph.getInput();
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

            FormattedNumber formatted = icuFormatter.format(valToCheck - offset);
            String match = rules.select(formatted);
            if (match.equals("other")) {
                match = "*";
            }
            return match.equals(key);
        }
    }

    private static LocalizedNumberFormatter formatterForOptions(Locale locale, Map<String, Object> fixedOptions) {
        UnlocalizedNumberFormatter nf;
        String skeleton = OptUtils.getString(fixedOptions, "skeleton");
        if (skeleton != null) {
            nf = NumberFormatter.forSkeleton(skeleton);
        } else {
            nf = NumberFormatter.with();
            Integer option = OptUtils.getInteger(fixedOptions, "minimumFractionDigits");
            if (option != null) {
                nf = nf.precision(Precision.minFraction(option));
            }

            option = OptUtils.getInteger(fixedOptions, "minimumIntegerDigits");
            if (option != null) {
                // TODO! Ask Shane.
            }
            option = OptUtils.getInteger(fixedOptions, "minimumFractionDigits");
            if (option != null) {
                nf = nf.precision(Precision.minFraction(option));
            }
            option = OptUtils.getInteger(fixedOptions, "maximumFractionDigits");
            if (option != null) {
                nf = nf.precision(Precision.maxFraction(option));
            }
            option = OptUtils.getInteger(fixedOptions, "minimumSignificantDigits");
            if (option != null) {
                nf = nf.precision(Precision.minSignificantDigits(option));
            }
            option = OptUtils.getInteger(fixedOptions, "maximumSignificantDigits");
            if (option != null) {
                nf = nf.precision(Precision.maxSignificantDigits(option));
            }

            String strOption = OptUtils.getString(fixedOptions, "signDisplay", "auto");
            SignDisplay signDisplay;
            switch (strOption) { 
                case "always": signDisplay = SignDisplay.ALWAYS; break;
                case "exceptZero": signDisplay = SignDisplay.EXCEPT_ZERO; break;
                case "negative": signDisplay = SignDisplay.NEGATIVE; break;
                case "never": signDisplay = SignDisplay.NEVER; break;
                case "auto": // intentional fall-through
                default:
                    signDisplay = SignDisplay.AUTO;
            }
            nf = nf.sign(signDisplay);

            GroupingStrategy grp;
            strOption = OptUtils.getString(fixedOptions, "useGrouping", "auto");
            switch (strOption) { 
                case "always": grp = GroupingStrategy.ON_ALIGNED; break; // TODO: check
                case "never": grp = GroupingStrategy.OFF; break;
                case "min2": grp = GroupingStrategy.MIN2; break;
                case "auto": // intentional fall-through
                default:
                    grp = GroupingStrategy.AUTO;
            }
            nf = nf.grouping(grp);
        }
        return nf.locale(locale);
    }
}
