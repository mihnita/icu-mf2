// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.message2x;

import java.util.Locale;
import java.util.Map;

/**
 * Takes an {@link MfDataModel} and formats it to a {@link String}
 * (and later on we will also implement formatting to a {@code FormattedMessage}).
 */
// TODO: move this in the MessageFormatter
class MfDataModelFormatter {

    @SuppressWarnings("unused")
    MfDataModelFormatter(MfDataModel dm, Locale locale, MfFunctionRegistry customFunctionRegistry) {
    }

    public String format(Map<String, Object> arguments) {
        return null;
    }
}
