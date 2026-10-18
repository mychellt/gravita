package br.gravita.core.domain.shared;

import java.util.regex.Pattern;

/** GTIN as the NF-e and product registry both accept it: GTIN-8, GTIN-12 (UPC-A), GTIN-13 (EAN-13) or GTIN-14 (DUN-14). */
public final class Gtin {

	private static final Pattern FORMAT = Pattern.compile("\\d{8}|\\d{12,14}");
	private static final Pattern ALL_ZEROS = Pattern.compile("0+");

	private Gtin() {
	}

	/** Suppliers write "SEM GTIN" or zeros when a product has no barcode; neither counts as a GTIN. */
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
