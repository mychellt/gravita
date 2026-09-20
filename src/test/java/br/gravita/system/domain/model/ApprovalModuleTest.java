package br.gravita.system.domain.model;

import br.gravita.core.domain.system.ApprovalModule;
import br.gravita.core.domain.system.UnknownApprovalModuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApprovalModuleTest {

	@ParameterizedTest
	@ValueSource(strings = {"purchasing", "sales", "finance", "PURCHASING", "Sales"})
	void shouldResolveEachOfTheThreeValidModules(String code) {
		assertThat(ApprovalModule.fromCode(code)).isNotNull();
	}

	@Test
	void shouldRejectUnknownModule() {
		assertThatThrownBy(() -> ApprovalModule.fromCode("logistics"))
				.isInstanceOf(UnknownApprovalModuleException.class)
				.hasMessageContaining("logistics");
	}

	@Test
	void shouldRejectNullModule() {
		assertThatThrownBy(() -> ApprovalModule.fromCode(null)).isInstanceOf(UnknownApprovalModuleException.class);
	}
}
