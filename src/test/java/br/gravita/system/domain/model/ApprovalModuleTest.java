package br.gravita.system.domain.model;

import br.gravita.core.domain.system.ApprovalModule;
import br.gravita.core.domain.system.UnknownApprovalModuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApprovalModuleTest {

	@ParameterizedTest
	@DisplayName("Resolves each of the three valid modules")
	@ValueSource(strings = {"purchasing", "sales", "finance", "PURCHASING", "Sales"})
	void shouldResolveEachOfTheThreeValidModules(String code) {
		assertThat(ApprovalModule.fromCode(code)).isNotNull();
	}

	@Test
	@DisplayName("Rejects an unknown module")
	void shouldRejectUnknownModule() {
		assertThatThrownBy(() -> ApprovalModule.fromCode("logistics"))
				.isInstanceOf(UnknownApprovalModuleException.class)
				.hasMessageContaining("logistics");
	}

	@Test
	@DisplayName("Rejects a null module")
	void shouldRejectNullModule() {
		assertThatThrownBy(() -> ApprovalModule.fromCode(null)).isInstanceOf(UnknownApprovalModuleException.class);
	}
}
