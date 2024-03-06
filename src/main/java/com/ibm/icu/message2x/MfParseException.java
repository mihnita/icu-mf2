// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2x;

/**
 * Used to report errors in {@link MessageFormatter} (parsing and validation).   
 */
public class MfParseException extends RuntimeException {
    private static final long serialVersionUID = -7634219305388292407L;

    MfParseException(String message) {
        super(message);
    }

    MfParseException(Throwable cause) {
        super(cause);
    }

    MfParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
