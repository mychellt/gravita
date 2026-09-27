package br.gravita.core.domain.tax;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.security.SecureRandom;
import java.time.YearMonth;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Shared SEFAZ access-key (chave de acesso) layout, used by both
 * {@link NfceAccessKeyGenerator} (model "65") and {@link NfeAccessKeyGenerator}
 * (model "55"): cUF(2) + AAMM(4) + CNPJ(14) + mod(2) + serie(3) + nNF(9) +
 * tpEmis(1) + cNF(8, random) + cDV(1, modulo-11 check digit).
 */
final class FiscalAccessKeyGenerator {

	private static final Pattern CNPJ_DIGITS = Pattern.compile("\\d{14}");
	private static final SecureRandom RANDOM = new SecureRandom();

	private static final Map<String, String> UF_CODES = Map.ofEntries(Map.entry("AC", "12"), Map.entry("AL", "27"),
			Map.entry("AP", "16"), Map.entry("AM", "13"), Map.entry("BA", "29"), Map.entry("CE", "23"),
			Map.entry("DF", "53"), Map.entry("ES", "32"), Map.entry("GO", "52"), Map.entry("MA", "21"),
			Map.entry("MT", "51"), Map.entry("MS", "50"), Map.entry("MG", "31"), Map.entry("PA", "15"),
			Map.entry("PB", "25"), Map.entry("PR", "41"), Map.entry("PE", "26"), Map.entry("PI", "22"),
			Map.entry("RJ", "33"), Map.entry("RN", "24"), Map.entry("RS", "43"), Map.entry("RO", "11"),
			Map.entry("RR", "14"), Map.entry("SC", "42"), Map.entry("SP", "35"), Map.entry("SE", "28"),
			Map.entry("TO", "17"));

	private FiscalAccessKeyGenerator() {
	}

	static String generate(String model, String issuerState, String cnpj, String documentSeries, long documentNumber,
			String emissionCode) {
		String ufCode = requireUfCode(issuerState);
		String cnpjDigits = requireCnpj(cnpj);
		String amCode = YearMonth.now().toString().replace("-", "").substring(2);
		String serie = zeroPad(documentSeries, 3);
		String number = zeroPad(Long.toString(documentNumber), 9);
		String randomCode = zeroPad(Integer.toString(RANDOM.nextInt(100_000_000)), 8);

		String first43 = ufCode + amCode + cnpjDigits + model + serie + number + emissionCode + randomCode;
		return first43 + checkDigit(first43);
	}

	private static String requireUfCode(String issuerState) {
		String code = issuerState == null ? null : UF_CODES.get(issuerState.trim().toUpperCase());
		if (code == null) {
			throw new BusinessRuleException("Unknown issuer state (UF): " + issuerState);
		}
		return code;
	}

	private static String requireCnpj(String cnpj) {
		if (cnpj == null || !CNPJ_DIGITS.matcher(cnpj).matches()) {
			throw new BusinessRuleException("CNPJ must be 14 digits: " + cnpj);
		}
		return cnpj;
	}

	private static String zeroPad(String value, int length) {
		if (value.length() > length) {
			throw new BusinessRuleException("Value " + value + " exceeds " + length + " digits");
		}
		return "0".repeat(length - value.length()) + value;
	}

	/**
	 * Modulo-11 check digit: weights cycle 2..9 from the rightmost digit;
	 * remainder &lt; 2 maps to 0, otherwise the digit is {@code 11 - remainder}.
	 */
	private static char checkDigit(String digits) {
		int sum = 0;
		int weight = 2;
		for (int i = digits.length() - 1; i >= 0; i--) {
			sum += (digits.charAt(i) - '0') * weight;
			weight = weight == 9 ? 2 : weight + 1;
		}
		int remainder = sum % 11;
		int dv = remainder < 2 ? 0 : 11 - remainder;
		return Character.forDigit(dv, 10);
	}
}
