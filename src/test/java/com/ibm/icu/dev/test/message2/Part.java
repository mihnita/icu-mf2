// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import java.util.Map;
import java.util.StringJoiner;

class Part {
    private final String type;
    private final String kind;
    private final String name;
    private final String value;
    Map<String, Object> options;

    Part(String type, String kind, String name, String value, Map<String, Object> options) {
        super();
        this.type = type;
        this.kind = kind;
        this.name = name;
        this.value = value;
        this.options = options;
    }

    @Override
    public String toString() {
        StringJoiner result = new StringJoiner(", ", "Part {", "}");
        result.add("type=" + Utilities.str(type));
        if (kind != null) {
            result.add("kind=" + Utilities.str(kind));
        }
        if (name != null) {
            result.add("kind=" + Utilities.str(name));
        }
        if (value != null) {
            result.add("kind=" + Utilities.str(value));
        }
        if (options != null) {
            result.add("options=" + options);
        }
        return result.toString();
    }
}
