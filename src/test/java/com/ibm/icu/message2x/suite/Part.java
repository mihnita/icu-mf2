package com.ibm.icu.message2x.suite;

import java.util.Map;
import java.util.StringJoiner;

import com.ibm.icu.message2x.Utilities;

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
