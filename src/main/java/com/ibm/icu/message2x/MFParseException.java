// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2x;

import java.text.ParseException;

/**
 * Used to report errors in {@link MessageFormatter} (parsing and validation).
 */
public class MFParseException extends ParseException {
    private static final long serialVersionUID = -7634219305388292407L;

    /**
     * Constructs a MFParseException with the specified message and offset.
     *
     * @param message the message
     * @param errorOffset the position where the error is found while parsing.
     */
    public MFParseException(String message, int errorOffset) {
        super(message, errorOffset);
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
