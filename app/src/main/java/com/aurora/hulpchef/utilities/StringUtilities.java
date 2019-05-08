package com.aurora.hulpchef.utilities;

import java.util.Locale;

public final class StringUtilities {
    private static final int MIN_DENOMINATOR_OF_FRACTIONS = 2;
    private static final int MAX_DENOMINATOR_OF_FRACTIONS = 10;
    private static final double ROUND_EPSILON = 0.05;


    private StringUtilities() {
        throw new IllegalStateException("Utility Class");
    }

    /**
     * Generates fraction from double
     *
     * @param quantity double to display
     * @return String containing the resulting quantity.
     */
    public static String toDisplayQuantity(double quantity) {
        if (isAlmostInteger(quantity)) {
            return "" + ((int) Math.round(quantity));
        }

        String baseString = "";
        double base = Math.floor(quantity);
        if ((int) base != 0) {
            baseString = "" + (int) base;
        }
        double remainder = quantity - base;

        for (int i = MIN_DENOMINATOR_OF_FRACTIONS; i <= MAX_DENOMINATOR_OF_FRACTIONS; i++) {
            if (isAlmostInteger(remainder * i)) {
                String remainderString = "" + ((int) Math.round(remainder * i) + "/" + i);
                if (!"".equals(baseString)) {
                    baseString += ", ";
                }
                return baseString + remainderString;
            }
        }

        // If all fails, just return double with 2 decimals (if needed)
        String doubleRepresentation;
        if (quantity == (long) quantity) {
            doubleRepresentation = String.format(Locale.ENGLISH, "%d", (long) quantity);
        } else {
            doubleRepresentation = String.format(Locale.ENGLISH, "%.2f", quantity);
        }

        return doubleRepresentation;
    }

    /**
     * returns true if the distance from the nearest int is smaller than {@value ROUND_EPSILON}
     *
     * @param quantity double to check
     * @return true when close enough.
     */
    private static boolean isAlmostInteger(double quantity) {
        return Math.abs(Math.round(quantity) - quantity) < ROUND_EPSILON * quantity;
    }
}
