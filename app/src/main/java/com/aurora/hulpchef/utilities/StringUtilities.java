package com.aurora.hulpchef.utilities;

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
        for (int i = MIN_DENOMINATOR_OF_FRACTIONS; i <= MAX_DENOMINATOR_OF_FRACTIONS; i++) {
            if (isAlmostInteger(quantity * i)) {
                return "" + ((int) Math.round(quantity * i) + "/" + i);
            }
        }
        return "" + quantity;
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
