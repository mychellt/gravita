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
}
