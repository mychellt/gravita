package br.gravita.core.domain.shared;

import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@NoArgsConstructor
public final class Gtin {

    private static final Pattern FORMAT = Pattern.compile("\\d{8}|\\d{12,14}");
    private static final Pattern ALL_ZEROS = Pattern.compile("0+");

    /**
     * Suppliers write "SEM GTIN" or zeros when a product has no barcode; neither counts as a GTIN.
     */
    public static boolean isValid(final String value) {
        return value != null && FORMAT.matcher(value).matches() && !ALL_ZEROS.matcher(value).matches();
    }

    /**
     * Whether the last digit is the correct GS1 check digit of the others. Needed to trust a code that was not declared as a
     * GTIN, such as a retailer's own product code that merely looks like one.
     */
    public static boolean hasValidCheckDigit(final String value) {
        if (!isValid(value)) {
            return false;
        }
        final int checkDigitIndex = value.length() - 1;
        int sum = 0;
        for (int i = 0; i < checkDigitIndex; i++) {
            final int weight = (checkDigitIndex - i) % 2 == 1 ? 3 : 1;
            sum += Character.digit(value.charAt(i), 10) * weight;
        }
        return (10 - sum % 10) % 10 == Character.digit(value.charAt(checkDigitIndex), 10);
    }
}
