// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2x;

import java.util.Map;

/**
 * The interface that must be implemented by all formatters
 * that can be used from {@link MessageFormatter}.
 *
 * @internal ICU 72 technology preview
 * TzuAt deprecated This API is for technology preview only.
 */
// TZUAT Deprecated
public interface Formatter {
    /**
     * A method that takes the object to format and returns
     * the i18n-aware string representation.
     *
     * @param toFormat the object to format.
     * @param variableOptions options that are not know at build time.
     * @return the formatted string.
     *
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    String formatToString(Object toFormat, Map<String, Object> variableOptions);

    /**
     * A method that takes the object to format and returns
     * the i18n-aware formatted placeholder.
     *
     * @param toFormat the object to format.
     * @param variableOptions options that are not know at build time.
     * @return the formatted placeholder.
     *
     * @internal ICU 72 technology preview
     * TzuAt deprecated This API is for technology preview only.
     */
    // TZUAT Deprecated
    FormattedPlaceholder format(Object toFormat, Map<String, Object> variableOptions);
}
