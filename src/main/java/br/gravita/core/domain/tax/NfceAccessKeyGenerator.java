package br.gravita.core.domain.tax;

/**
 * Builds the 44-digit NFC-e access key (chave de acesso) per the SEFAZ
 * layout - see {@link FiscalAccessKeyGenerator}, shared with
 * {@link NfeAccessKeyGenerator}. This is the real, independently verifiable
 * part of UC-M3-04's "SEFAZ-UF client adapter" - it needs no network access,
 * unlike the XML submission itself.
 */
public final class NfceAccessKeyGenerator {

	private static final String NFCE_MODEL = "65";

	private NfceAccessKeyGenerator() {
	}

	public static String generate(String issuerState, String cnpj, String documentSeries, long documentNumber,
			EmissionType emissionType) {
		return FiscalAccessKeyGenerator.generate(NFCE_MODEL, issuerState, cnpj, documentSeries, documentNumber,
				emissionType.code());
	}

	public enum EmissionType {
		NORMAL("1"),
		CONTINGENCY("9");

		private final String code;

		EmissionType(String code) {
			this.code = code;
		}

		public String code() {
			return code;
		}
	}
}
