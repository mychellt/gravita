package br.gravita.tax.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.tax.DiscriminationTemplate;
import br.gravita.core.domain.tax.DiscriminationTemplateId;
import br.gravita.core.domain.tax.ServiceCode;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DiscriminationTemplateTest {

	private static final DiscriminationTemplateId ID = DiscriminationTemplateId.of(UUID.randomUUID());

	@Test
	@DisplayName("Creates and updates a template while keeping its identity")
	void createsAndUpdatesKeepingIdentity() {
		DiscriminationTemplate template = DiscriminationTemplate.of(ID, ServiceCode.of("1.05"), "Licenciamento");

		template.update(ServiceCode.of("0801"), "Treinamento");

		assertThat(template.getId()).isEqualTo(ID);
		assertThat(template.getServiceCode().value()).isEqualTo("08.01");
		assertThat(template.getTemplateText()).isEqualTo("Treinamento");
	}

	@Test
	@DisplayName("Rejects a missing service code and blank text")
	void rejectsMissingServiceCodeAndBlankText() {
		assertThatThrownBy(() -> DiscriminationTemplate.of(ID, null, "x")).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> DiscriminationTemplate.of(ID, ServiceCode.of("01.05"), " "))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Leaves the template unchanged when an update fails")
	void aFailedUpdateLeavesTheTemplateUnchanged() {
		DiscriminationTemplate template = DiscriminationTemplate.of(ID, ServiceCode.of("01.05"), "keep");

		assertThatThrownBy(() -> template.update(ServiceCode.of("02.01"), "")).isInstanceOf(BusinessRuleException.class);

		assertThat(template.getServiceCode().value()).isEqualTo("01.05");
		assertThat(template.getTemplateText()).isEqualTo("keep");
	}
}
