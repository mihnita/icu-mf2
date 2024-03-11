// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2x;

import com.ibm.icu.text.DateFormat;
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
import java.util.TimeZone;

/**
 * Creates a {@link Formatter} doing formatting of date / time, similar to
 * <code>{exp, date}</code> and <code>{exp, time}</code> in {@link com.ibm.icu.text.MessageFormat}.
 */
class DateTimeFormatterFactory implements FormatterFactory {

    private static int stringToStyle(String option) {
        switch (option) {
            case "full":
                return DateFormat.FULL;
            case "long":
                return DateFormat.LONG;
            case "medium":
                return DateFormat.MEDIUM;
            case "short":
                return DateFormat.SHORT;
            default:
                throw new IllegalArgumentException("Invalid datetime style: " + option);
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
        String opt;

        int dateStyle = DateFormat.NONE;
        opt = OptUtils.getString(fixedOptions, "dateStyle");
        if (opt != null) {
            dateStyle = stringToStyle(opt);
        }

        int timeStyle = DateFormat.NONE;
        opt = OptUtils.getString(fixedOptions, "timeStyle");
        if (opt != null) {
            timeStyle = stringToStyle(opt);
        }

        // TODO: how to handle conflicts. What if we have both skeleton and style, or pattern?
        if (dateStyle == DateFormat.NONE && timeStyle == DateFormat.NONE) {
            String skeleton = checkForFieldOptions(fixedOptions);
            if (skeleton.isEmpty()) {
                // Custom option, icu namespace
                skeleton = OptUtils.getString(fixedOptions, "icu:skeleton", "");
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

    private static String checkForFieldOptions(Map<String, Object> options) {
        StringBuilder skeleton = new StringBuilder();
        String opt;

        // In all the switches below we just ignore invalid options.
        // Would be nice to report (log?), but ICU does not have a clear policy on how to do that.
        // But we don't want to throw, that is too drastic.

        opt = OptUtils.getString(options, "weekday", "");
        switch (opt) {
            case "long":
                skeleton.append("EEEE");
                break;
            case "short":
                skeleton.append("E");
                break;
            case "narrow":
                skeleton.append("EEEEEE");
                break;
            default:
                // invalid value, we just ignore it.
        }

        opt = OptUtils.getString(options, "era", "");
        switch (opt) {
            case "long":
                skeleton.append("GGGG");
                break;
            case "short":
                skeleton.append("G");
                break;
            case "narrow":
                skeleton.append("GGGGG");
                break;
            default:
                // invalid value, we just ignore it.
        }

        opt = OptUtils.getString(options, "year", "");
        switch (opt) {
            case "numeric":
                skeleton.append("y");
                break;
            case "2-digit":
                skeleton.append("yy");
                break;
            default:
                // invalid value, we just ignore it.
        }

        opt = OptUtils.getString(options, "month", "");
        switch (opt) {
            case "numeric":
                skeleton.append("M");
                break;
            case "2-digit":
                skeleton.append("MM");
                break;
            case "long":
                skeleton.append("MMMM");
                break;
            case "short":
                skeleton.append("MMM");
                break;
            case "narrow":
                skeleton.append("MMMMM");
                break;
            default:
                // invalid value, we just ignore it.
        }

        opt = OptUtils.getString(options, "day", "");
        switch (opt) {
            case "numeric":
                skeleton.append("d");
                break;
            case "2-digit":
                skeleton.append("dd");
                break;
            default:
                // invalid value, we just ignore it.
        }

        int showHour = 0;
        opt = OptUtils.getString(options, "hour", "");
        switch (opt) {
            case "numeric":
                showHour = 1;
                break;
            case "2-digit":
                showHour = 2;
                break;
            default:
                // invalid value, we just ignore it.
        }
        if (showHour > 0) {
            String hourCycle = "";
            opt = OptUtils.getString(options, "hourCycle", "");
            switch (opt) {
                case "h11":
                    hourCycle = "K";
                    break;
                case "h12":
                    hourCycle = "h";
                    break;
                case "h23":
                    hourCycle = "H";
                    break;
                case "h24":
                    hourCycle = "k";
                    break;
                default:
                    hourCycle = "j"; // default for the locale
            }
            skeleton.append(hourCycle);
            if (showHour == 2) {
                skeleton.append(hourCycle);
            }
        }

        opt = OptUtils.getString(options, "minute", "");
        switch (opt) {
            case "numeric":
                skeleton.append("m");
                break;
            case "2-digit":
                skeleton.append("mm");
                break;
            default:
                // invalid value, we just ignore it.
        }

        opt = OptUtils.getString(options, "second", "");
        switch (opt) {
            case "numeric":
                skeleton.append("s");
                break;
            case "2-digit":
                skeleton.append("ss");
                break;
            default:
                // invalid value, we just ignore it.
        }

        opt = OptUtils.getString(options, "fractionalSecondDigits", "");
        switch (opt) {
            case "1":
                skeleton.append("S");
                break;
            case "2":
                skeleton.append("SS");
                break;
            case "3":
                skeleton.append("SSS");
                break;
            default:
                // invalid value, we just ignore it.
        }

        opt = OptUtils.getString(options, "timeZoneName", "");
        switch (opt) {
            case "long":
                skeleton.append("z");
                break;
            case "short":
                skeleton.append("zzzz");
                break;
            case "shortOffset":
                skeleton.append("O");
                break;
            case "longOffset":
                skeleton.append("OOOO");
                break;
            case "shortGeneric":
                skeleton.append("v");
                break;
            case "longGeneric":
                skeleton.append("vvvv");
                break;
            default:
                // invalid value, we just ignore it.
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
                    return new FormattedPlaceholder(
                            toFormat, new PlainStringFormattedValue(toFormat.toString()));
                }
            }
            if (toFormat instanceof Calendar) {
                TimeZone tz = ((Calendar) toFormat).getTimeZone();
                long milis = ((Calendar) toFormat).getTimeInMillis();
                com.ibm.icu.util.TimeZone icuTz = com.ibm.icu.util.TimeZone.getTimeZone(tz.getID());
                com.ibm.icu.util.Calendar calendar =
                        com.ibm.icu.util.Calendar.getInstance(icuTz, locale);
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
        } catch (Exception e) {
            // just ignore, we want to try more
        }

        try {
            LocalDateTime ldt = LocalDateTime.parse(text);
            return new Date(
                    ldt.getYear() - 1900,
                    ldt.getMonthValue(),
                    ldt.getDayOfMonth(),
                    ldt.getHour(),
                    ldt.getMinute(),
                    ldt.getSecond());
        } catch (Exception e) {
            // just ignore, we want to try more
        }

        try {
            LocalTime lt = LocalTime.parse(text);
            LocalDateTime ldt = LocalDateTime.of(LocalDate.now(), lt);
            return new Date(
                    ldt.getYear() - 1900,
                    ldt.getMonthValue(),
                    ldt.getDayOfMonth(),
                    ldt.getHour(),
                    ldt.getMinute(),
                    ldt.getSecond());
        } catch (Exception e) {
            // just ignore, we want to try more
        }

        try {
            Instant instant = Instant.parse(text);
            return new Date(instant.toEpochMilli());
        } catch (Exception e) {
            // just ignore, we want to try more
        }

        try {
            OffsetDateTime odt = OffsetDateTime.parse(text);
            return GregorianCalendar.from(odt.toZonedDateTime());
        } catch (Exception e) {
            // just ignore, we want to try more
        }

        try {
            OffsetTime ot = OffsetTime.parse(text);
            return GregorianCalendar.from(OffsetDateTime.from(ot).toZonedDateTime());
        } catch (Exception e) {
            // just ignore, we want to try more
        }

        try {
            ZonedDateTime zdt = ZonedDateTime.parse(text);
            return GregorianCalendar.from(zdt);
        } catch (Exception e) {
            // just ignore, we want to try more
        }

        return text;
    }
}
