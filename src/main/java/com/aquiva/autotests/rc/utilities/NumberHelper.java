package com.aquiva.autotests.rc.utilities;

/**
 * Utility class that provides useful functions that work with numbers.
 * E.g. number conversion.
 */
public class NumberHelper {

    /**
     * Convert from Double to Integer with null-safety.
     *
     * @param doubleValue any Double object to convert
     * @return the {@code double} value represented by the given object
     * converted to type {@code Integer},
     * or {@code null} if the given object is {@code null}.
     */
    public static Integer doubleToInteger(Double doubleValue) {
        return doubleValue != null ? doubleValue.intValue() : null;
    }

    /**
     * Convert from Double to Integer to String with null-safety.
     *
     * @param doubleValue any Double object to convert
     * @return the {@code double} value represented by the given object
     * converted to {@link Integer#toString()},
     * or {@code "null"} if the given object is {@code null}.
     */
    public static String doubleToIntToString(Double doubleValue) {
        return String.valueOf(doubleToInteger(doubleValue));
    }

    /**
     * Get the string value of the double number without trailing zeros
     * and decimal point if the decimal part is zero.
     * (e.g. 1.0 -> "1", 1.20 -> "1.2").
     *
     * @param doubleValue a double value to convert
     * @return the string value of the double number without trailing zeros and decimal point if the decimal part is zero
     */
    public static String getNumberValueWithoutTrailingZeros(Double doubleValue) {
        return String.valueOf(doubleValue).replaceAll("\\.0*$", "");
    }
}
