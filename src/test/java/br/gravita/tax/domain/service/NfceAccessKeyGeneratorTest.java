package br.gravita.tax.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.tax.NfceAccessKeyGenerator;
import br.gravita.core.domain.tax.NfceAccessKeyGenerator.EmissionType;
import java.time.YearMonth;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class NfceAccessKeyGeneratorTest {

	private static final String CNPJ = "11222333000181";
	private static final Pattern DIGITS_44 = Pattern.compile("\\d{44}");

	@Test
	void aGeneratedKeyIs44DigitsLongAndEncodesUfMonthCnpjModelSeriesAndNumber() {
		String key = NfceAccessKeyGenerator.generate("SP", CNPJ, "1", 42L, EmissionType.NORMAL);

		assertThat(key).matches(DIGITS_44);
		assertThat(key.substring(0, 2)).isEqualTo("35");
		assertThat(key.substring(2, 6)).isEqualTo(YearMonth.now().toString().replace("-", "").substring(2));
		assertThat(key.substring(6, 20)).isEqualTo(CNPJ);
		assertThat(key.substring(20, 22)).isEqualTo("65");
		assertThat(key.substring(22, 25)).isEqualTo("001");
		assertThat(key.substring(25, 34)).isEqualTo("000000042");
		assertThat(key.substring(34, 35)).isEqualTo("1");
	}

	@Test
	void aContingencyKeyEncodesTpEmisNine() {
		String key = NfceAccessKeyGenerator.generate("SP", CNPJ, "1", 42L, EmissionType.CONTINGENCY);

		assertThat(key.substring(34, 35)).isEqualTo("9");
	}

	@Test
	void theCheckDigitIsValidUnderModulo11() {
		String key = NfceAccessKeyGenerator.generate("RJ", CNPJ, "2", 999L, EmissionType.NORMAL);

		assertThat(key.charAt(43)).isEqualTo(expectedCheckDigit(key.substring(0, 43)));
	}

	@Test
	void twoKeysForTheSameInputsDifferInTheRandomSegmentOrCheckDigit() {
		String first = NfceAccessKeyGenerator.generate("SP", CNPJ, "1", 1L, EmissionType.NORMAL);
		String second = NfceAccessKeyGenerator.generate("SP", CNPJ, "1", 1L, EmissionType.NORMAL);

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void anUnknownUfIsRejected() {
		assertThatThrownBy(() -> NfceAccessKeyGenerator.generate("ZZ", CNPJ, "1", 1L, EmissionType.NORMAL))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void aCnpjThatIsNot14DigitsIsRejected() {
		assertThatThrownBy(() -> NfceAccessKeyGenerator.generate("SP", "123", "1", 1L, EmissionType.NORMAL))
				.isInstanceOf(BusinessRuleException.class);
	}

	private char expectedCheckDigit(String first43Digits) {
		int sum = 0;
		int weight = 2;
		for (int i = first43Digits.length() - 1; i >= 0; i--) {
			sum += (first43Digits.charAt(i) - '0') * weight;
			weight = weight == 9 ? 2 : weight + 1;
		}
		int remainder = sum % 11;
		int dv = remainder < 2 ? 0 : 11 - remainder;
		return Character.forDigit(dv, 10);
	}
}
