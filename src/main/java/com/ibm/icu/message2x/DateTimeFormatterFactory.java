// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2x;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import com.ibm.icu.text.DateFormat;

/**
 * Creates a {@link Formatter} doing formatting of date / time, similar to
 * <code>{exp, date}</code> and <code>{exp, time}</code> in {@link com.ibm.icu.text.MessageFormat}.
 */
class DateTimeFormatterFactory implements FormatterFactory {

    private static int stringToStyle(String option) {
        switch (option) {
            case "full": return DateFormat.FULL;
            case "long": return DateFormat.LONG;
            case "medium": return DateFormat.MEDIUM;
            case "short": return DateFormat.SHORT;
            default: throw new IllegalArgumentException("Invalid datetime style: " + option);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException when something goes wrong
     *         (for example conflicting options, invalid option values, etc.)
     */
    @Override
    public Formatter createFormatter(Locale locale, Map<String, Object> fixedOptions) {
//        DateFormat df;
        String opt;

        int dateStyle = DateFormat.NONE;
        opt = getOptionAsString(fixedOptions, "dateStyle");
        if (!opt.isEmpty()) {
            dateStyle = stringToStyle(opt);
        }

        int timeStyle = DateFormat.NONE;
        opt = getOptionAsString(fixedOptions, "timeStyle");
        if (!opt.isEmpty()) {
            timeStyle = stringToStyle(opt);
        }

        // TODO: how to handle conflicts. What if we have both skeleton and style, or pattern?
        if (dateStyle == DateFormat.NONE && timeStyle == DateFormat.NONE) {
            String skeleton = checkForFieldOptions(fixedOptions);
            if (skeleton.isEmpty()) {
                // Custom option, icu namespace
                skeleton = getOptionAsString(fixedOptions, "icu:skeleton");
            }
            if (!skeleton.isEmpty()) {
                DateFormat df = DateFormat.getInstanceForSkeleton(skeleton, locale);
                return new DateTimeFormatter(locale, df);
            }

            // No skeletons, custom or otherwise, match fallback to short / short as per spec.
            dateStyle = DateFormat.SHORT;
            timeStyle = DateFormat.SHORT;
        }

        DateFormat df = DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);
        return new DateTimeFormatter(locale, df);
    }

    private static String getOptionAsString(Map<String, Object> fixedOptions, String key) {
        Object opt = fixedOptions.get(key);
        if (opt != null) {
            return Objects.toString(opt);
        }
        return "";
    }

    private static String checkForFieldOptions(Map<String, Object> options) {
        StringBuilder skeleton = new StringBuilder();
        String opt;

        opt = getOptionAsString(options, "weekday");
        switch (opt) {
            case "long": skeleton.append("EEEE"); break;
            case "short": skeleton.append("E"); break;
            case "narrow": skeleton.append("EEEEEE"); break;
        }

        opt = getOptionAsString(options, "era");
        switch (opt) {
            case "long": skeleton.append("GGGG"); break;
            case "short": skeleton.append("G"); break;
            case "narrow": skeleton.append("GGGGG"); break;
        }

        opt = getOptionAsString(options, "year");
        switch (opt) {
            case "numeric": skeleton.append("y"); break;
            case "2-digit": skeleton.append("yy"); break;
        }

        opt = getOptionAsString(options, "month");
        switch (opt) {
            case "numeric": skeleton.append("M"); break;
            case "2-digit": skeleton.append("MM"); break;
            case "long": skeleton.append("MMMM"); break;
            case "short": skeleton.append("MMM"); break;
            case "narrow": skeleton.append("MMMMM"); break;
        }

        opt = getOptionAsString(options, "day");
        switch (opt) {
            case "numeric": skeleton.append("d"); break;
            case "2-digit": skeleton.append("dd"); break;
        }

        int showHour = 0; 
        opt = getOptionAsString(options, "hour");
        switch (opt) {
            case "numeric": showHour = 1; break;
            case "2-digit": showHour = 2; break;
        }
        if (showHour > 0) {
            String hourCycle = "";
            opt = getOptionAsString(options, "hourCycle");
            switch (opt) {
                case "h11": hourCycle = "K"; break;
                case "h12": hourCycle = "h"; break;
                case "h23": hourCycle = "H"; break;
                case "h24": hourCycle = "k"; break;
                default:
                    hourCycle = "j"; // default for the locale
            }
            skeleton.append(hourCycle);
            if (showHour == 2) {
                skeleton.append(hourCycle);
            }
        }

        opt = getOptionAsString(options, "minute");
        switch (opt) {
            case "numeric": skeleton.append("m"); break;
            case "2-digit": skeleton.append("mm"); break;
        }

        opt = getOptionAsString(options, "second");
        switch (opt) {
            case "numeric": skeleton.append("s"); break;
            case "2-digit": skeleton.append("ss"); break;
        }

        opt = getOptionAsString(options, "fractionalSecondDigits");
        switch (opt) {
            case "1": skeleton.append("S"); break;
            case "2": skeleton.append("SS"); break;
            case "3": skeleton.append("SSS"); break;
        }

        opt = getOptionAsString(options, "timeZoneName");
        switch (opt) {
            case "long": skeleton.append("z"); break;
            case "short": skeleton.append("zzzz"); break;
            case "shortOffset": skeleton.append("O"); break;
            case "longOffset": skeleton.append("OOOO"); break;
            case "shortGeneric": skeleton.append("v"); break;
            case "longGeneric": skeleton.append("vvvv"); break;
        }

        return skeleton.toString();
    }

    private static class DateTimeFormatter implements Formatter {
        private final DateFormat icuFormatter;
        private final Locale locale;

        private DateTimeFormatter(Locale locale, DateFormat df) {
            this.locale = locale;
            this.icuFormatter = df;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public FormattedPlaceholder format(Object toFormat, Map<String, Object> variableOptions) {
            // TODO: use a special type to indicate function without input argument.
            if (toFormat == null) {
                throw new IllegalArgumentException("The date to format can't be null");
            }
            if (toFormat instanceof CharSequence) {
                toFormat = parseIso8601(toFormat.toString());
                if (toFormat instanceof CharSequence) {
                    return new FormattedPlaceholder(toFormat, new PlainStringFormattedValue(toFormat.toString()));
                }
            }
            if (toFormat instanceof Calendar) {
                TimeZone tz = ((Calendar) toFormat).getTimeZone();
                long milis = ((Calendar) toFormat).getTimeInMillis();
                com.ibm.icu.util.TimeZone icuTz = com.ibm.icu.util.TimeZone.getTimeZone(tz.getID());
                com.ibm.icu.util.Calendar calendar = com.ibm.icu.util.Calendar.getInstance(icuTz, locale);
                calendar.setTimeInMillis(milis);
                toFormat = calendar;
            }
            String result = icuFormatter.format(toFormat);
            return new FormattedPlaceholder(toFormat, new PlainStringFormattedValue(result));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String formatToString(Object toFormat, Map<String, Object> variableOptions) {
            return format(toFormat, variableOptions).toString();
        }
    }

    // TODO: make this better
    private static Object parseIso8601(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        try {
            LocalDate ld = LocalDate.parse(text);
            return new Date(ld.getYear() - 1900, ld.getMonthValue(), ld.getDayOfMonth());
        } catch (Exception e) {}

        try {
            LocalDateTime ldt = LocalDateTime.parse(text);
            return new Date(ldt.getYear() - 1900, ldt.getMonthValue(), ldt.getDayOfMonth(),
                    ldt.getHour(), ldt.getMinute(), ldt.getSecond());
        } catch (Exception e) {}

        try {
            LocalTime lt = LocalTime.parse(text);
            LocalDateTime ldt = LocalDateTime.of(LocalDate.now(), lt);
            return new Date(ldt.getYear() - 1900, ldt.getMonthValue(), ldt.getDayOfMonth(),
                    ldt.getHour(), ldt.getMinute(), ldt.getSecond());
        } catch (Exception e) {}

        try {
            Instant instant = Instant.parse(text);
            return new Date(instant.toEpochMilli());
        } catch (Exception e) {}

        try {
            OffsetDateTime odt = OffsetDateTime.parse(text);
            return GregorianCalendar.from(odt.toZonedDateTime());
        } catch (Exception e) {}

        try {
            OffsetTime ot = OffsetTime.parse(text);
            return GregorianCalendar.from(OffsetDateTime.from(ot).toZonedDateTime());
        } catch (Exception e) {}

        try {
            ZonedDateTime zdt = ZonedDateTime.parse(text);
            return GregorianCalendar.from(zdt);
        } catch (Exception e) {}

        return text;
    }
}
