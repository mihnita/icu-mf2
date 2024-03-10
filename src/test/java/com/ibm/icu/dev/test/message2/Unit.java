// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

class Unit {
    final String src;
    final String exp;
    final String cleanSrc;
    final Map<String, Object> params;
    final List<Part> parts;

    Unit(String src, String cleanSrc, Map<String, Object> params, String exp, List<Part> parts) {
        super();
        this.src = src;
        this.cleanSrc = cleanSrc;
        this.params = params;
        this.exp = exp;
        this.parts = parts;
    }

    @Override
    public String toString() {
        StringJoiner result = new StringJoiner(", ", "UnitTest {", "}");
        result.add("src=" + Utilities.str(src));
        if (cleanSrc != null) {
            result.add("cleanSrc=" + Utilities.str(cleanSrc));
        }
        if (params != null) {
            result.add("params=" + params);
        }
        if (exp != null) {
            result.add("exp=" + Utilities.str(exp));
        }
        if (parts != null) {
            result.add("parts=" + parts);
        }
        return result.toString();
    }

}