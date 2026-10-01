package br.gravita.tax.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.tax.ServiceCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ServiceCodeTest {

	@ParameterizedTest
	@ValueSource(strings = { "01.05", "1.05", "0105", " 1.05 " })
	void normalizesToTheCanonicalLc116Form(String raw) {
		assertThat(ServiceCode.of(raw).value()).isEqualTo("01.05");
	}

	@Test
	void acceptsTheLastItemOfTheList() {
		assertThat(ServiceCode.of("40.01").value()).isEqualTo("40.01");
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "  ", "abc", "41.01", "00.01", "01.00", "1.5", "01.050", "01-05" })
	void rejectsCodesOutsideTheLc116Structure(String raw) {
		assertThatThrownBy(() -> ServiceCode.of(raw)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void rejectsNull() {
		assertThatThrownBy(() -> ServiceCode.of(null)).isInstanceOf(BusinessRuleException.class);
	}
}
