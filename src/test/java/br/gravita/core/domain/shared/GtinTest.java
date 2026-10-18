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

	@Test
	@DisplayName("does not trim: callers normalise before asking")
	void doesNotTrim() {
		assertThat(Gtin.isValid(" 7891234567895")).isFalse();
	}
}
