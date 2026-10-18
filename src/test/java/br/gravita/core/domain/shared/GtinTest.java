package br.gravita.core.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class GtinTest {

	@ParameterizedTest
	@ValueSource(strings = {"96385074", "036000291452", "7891234567895", "17891234567892"})
	@DisplayName("accepts GTIN-8, GTIN-12, GTIN-13 and GTIN-14")
	void acceptsTheGtinLengths(final String value) {
		assertThat(Gtin.isValid(value)).isTrue();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"SEM GTIN", "1234567", "123456789", "12345678901", "123456789012345", "78912345678AB", "00000000", "0000000000000"})
	@DisplayName("rejects a missing, malformed, wrongly sized or all-zero value")
	void rejectsWhatIsNotAGtin(final String value) {
		assertThat(Gtin.isValid(value)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = {"7891234567895", "7908597504167", "7891019907892", "96385074", "036000291452", "17891234567892"})
	@DisplayName("recognises the correct check digit of GTIN-8, 12, 13 and 14")
	void recognisesTheCorrectCheckDigit(final String value) {
		assertThat(Gtin.hasValidCheckDigit(value)).isTrue();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"7891234567890", "7908597504160", "96385075", "5663637", "SEM GTIN"})
	@DisplayName("rejects a wrong check digit and anything that is not a GTIN")
	void rejectsAWrongCheckDigit(final String value) {
		assertThat(Gtin.hasValidCheckDigit(value)).isFalse();
	}

	@Test
	@DisplayName("does not trim: callers normalise before asking")
	void doesNotTrim() {
		assertThat(Gtin.isValid(" 7891234567895")).isFalse();
	}
}
