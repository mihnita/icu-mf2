package com.ibm.icu.message2x;

import java.util.Locale;
import java.util.Map;

public class MessageFormatter {
    final MfDataModel.Message message;
    final Locale locale;
    final String pattern;

    public MessageFormatter(MfDataModel.Message message, Locale locale, String pattern) {
        super();
        this.message = message;
        this.locale = locale;
        this.pattern = pattern;
    }

    public String format(Map<String, Object> arguments) {
        return "";
    }

    public String formatToString(Map<String, Object> arguments) {
        return "";
    }

    public MfDataModel.Message getDataModel() {
        return message;
    }

    public Locale getLocale() {
        return locale;
    }

    public String getPattern() {
        return pattern;
    }

    public static class Builder {
        Locale locale;
        String pattern;

        MessageFormatter build() {
            MfDataModel.Message dataModel = Parser.parse(pattern);
            return new MessageFormatter(dataModel, locale, pattern);
        }

        // Builder setDataModel(Mf2DataModel dataModel) {
        //
        // }
        // Builder setFunctionRegistry(Mf2FunctionRegistry functionRegistry) {
        //
        // }
        Builder setLocale(Locale locale) {
            this.locale = locale;
            return this;
        }

        Builder setPattern(String pattern) {
            this.pattern = pattern;
            return this;
        }
    }
}
