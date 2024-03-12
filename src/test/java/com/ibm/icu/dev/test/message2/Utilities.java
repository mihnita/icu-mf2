// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

class Utilities {

    static String str(String str) {
        if (str == null) {
            return "null";
        }

        StringBuilder result = new StringBuilder();
        str.chars().forEach(c -> {
            switch (c) {
                case '\\':
                    result.append("\\\\");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                case '\n':
                    result.append("\\n");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                default:
                    if (c < 0x0020 || (c >= 0x3000 && c <= 3020)) {
                        result.append(String.format("\\u%04X", c));
                    } else {
                        result.append((char) c);
                    }
            }
        });
        return "\"" + result.toString() + "\"";
    }

    static Path getTestFile(Class<?> cls, String fileName) throws URISyntaxException {
        URI getPath = cls.getClassLoader().getResource("").toURI();
        Path filePath = Paths.get(getPath);
        // System.out.println(getPath);
        // System.out.println(filePath);
        Path json = Paths.get(fileName);
        return filePath.resolve(json);
    }
}
