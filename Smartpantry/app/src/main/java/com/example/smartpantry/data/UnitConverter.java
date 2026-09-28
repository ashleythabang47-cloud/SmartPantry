package com.example.smartpantry.data;

import java.util.Locale;

/**
 * Converts user-friendly units into the base units stored in the database:
 * mass -> g, volume -> ml, count -> pcs.
 * Use SUPPORTED_UNITS to fill the unit Spinner on the Add/Edit screen.
 */
public final class UnitConverter {

    public static final String[] SUPPORTED_UNITS =
            {"g", "kg", "ml", "l", "tsp", "tbsp", "cup", "pcs"};

    private UnitConverter() { }

    /** Returns the quantity converted to its base unit. */
    public static double toBase(double quantity, String unit) {
        switch (unit.toLowerCase(Locale.ROOT)) {
            case "kg":   return quantity * 1000;
            case "l":    return quantity * 1000;
            case "tsp":  return quantity * 5;
            case "tbsp": return quantity * 15;
            case "cup":  return quantity * 250;
            default:     return quantity; // g, ml, pcs
        }
    }

    /** Returns the base unit ("g", "ml" or "pcs") for a user-selected unit. */
    public static String baseUnit(String unit) {
        switch (unit.toLowerCase(Locale.ROOT)) {
            case "g":
            case "kg":
                return "g";
            case "ml":
            case "l":
            case "tsp":
            case "tbsp":
            case "cup":
                return "ml";
            default:
                return "pcs";
        }
    }
}