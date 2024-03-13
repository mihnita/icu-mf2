// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

class Unit {
    final String src;
    final List<String> srcs;
    final String locale;
    final Map<String, Object> params;
    final String exp;
    final String ignore;
    final List<Error> errors;

    Unit(String src, List<String> srcs, String locale, Map<String, Object> params, String exp, String ignore, List<Error> errors) {
        this.src = src;
        this.srcs = srcs;
        this.locale = locale;
        this.params = params;
        this.exp = exp;
        this.ignore = ignore;
        this.errors = errors;
    }

    class Error {
        final String name;
        final String type;
        Error(String name, String type) {
            this.name = name;
            this.type = type;
        }
        @Override
        public String toString() {
            return "Error [" + (name != null ? "name=" + name + ", " : "") + (type != null ? "type=" + type : "") + "]";
        }
    }

    @Override
    public String toString() {
        StringJoiner result = new StringJoiner(", ", "UnitTest {", "}");
        result.add("src=" + Utilities.str(src));
        if (params != null) {
            result.add("params=" + params);
        }
        if (exp != null) {
            result.add("exp=" + Utilities.str(exp));
        }
        return result.toString();
    }

    public Unit merge(Unit other) {
        String newSrc = other.src != null ? other.src : this.src;
        List<String> newSrcs = other.srcs != null ? other.srcs : this.srcs;
        String newLocale = other.locale != null ? other.locale : this.locale;
        Map<String, Object> newParams = other.params != null ? other.params : this.params;
        String newExp = other.exp != null ? other.exp : this.exp;
        String newIgnore = other.ignore != null ? other.ignore : this.ignore;
        List<Error> newErrors = other.errors != null ? other.errors : this.errors;
        return new Unit(newSrc, newSrcs, newLocale, newParams, newExp, newIgnore, newErrors);
    }
}
