package br.gravita.core.domain.tax;

/**
 * Builds the 44-digit NFe (modelo 55) access key - see
 * {@link FiscalAccessKeyGenerator}, shared with {@link NfceAccessKeyGenerator}.
 * UC-M2-01 only ever issues online (normal emission); contingency emission
 * for NFe is UC-M2-03's concern.
 */
public final class NfeAccessKeyGenerator {

	private static final String NFE_MODEL = "55";
	private static final String NORMAL_EMISSION_CODE = "1";

	private NfeAccessKeyGenerator() {
	}

	public static String generate(String issuerState, String cnpj, String documentSeries, long documentNumber) {
		return FiscalAccessKeyGenerator.generate(NFE_MODEL, issuerState, cnpj, documentSeries, documentNumber,
				NORMAL_EMISSION_CODE);
	}
}
