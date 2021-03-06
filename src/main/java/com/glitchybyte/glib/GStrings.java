// Copyright 2014-2020 GlitchyByte
// SPDX-License-Identifier: Apache-2.0

package com.glitchybyte.glib;

import java.text.DecimalFormat;
import java.text.Format;
import java.util.Locale;

/**
 * String utilities.
 */
public final class GStrings {

    /**
     * System new line separator.
     */
    public static final String NEW_LINE = System.lineSeparator();

    /**
     * Spaces for indentation.
     */
    public static final String SPACE_TAB = "  ";

    /**
     * Number format for minimal human representation of double as String.
     */
    private static final Format numberFormat = DecimalFormat.getNumberInstance(Locale.US);

    /**
     * Indents the string. It will correctly indent multiple lines
     * as long as they have system line separators.
     *
     * @param str String to indent.
     * @return Indented string.
     */
    public static String indent(final String str) {
        return str.contains(NEW_LINE) ?
                SPACE_TAB + str.replaceAll(NEW_LINE, NEW_LINE + SPACE_TAB) :
                SPACE_TAB + str;
    }

    /**
     * Convenience replacement for String.format, but it already includes US locale.
     *
     * @param format A format string.
     * @param args Arguments for format.
     * @return Formatted string.
     */
    public static String format(final String format, final Object... args) {
        return String.format(Locale.US, format, args);
    }

    /**
     * Convenience double to String formatter with human minimal representation.
     *
     * @param value Double to format into a String.
     * @return Minimal String representation of the value.
     */
    public static String fromDouble(final double value) {
        return numberFormat.format(value);
    }

    /**
     * Convenience float to String formatter with human minimal representation.
     *
     * @param value Float to format into a String.
     * @return Minimal String representation of the value.
     */
    public static String fromFloat(final float value) {
        return numberFormat.format(value);
    }

    private GStrings() {
        // Hiding constructor.
    }
}
