package br.gravita.system.domain.model;

import br.gravita.core.domain.system.ApprovalAlcada;
import br.gravita.core.domain.system.ApprovalModule;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.shared.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApprovalAlcadaTest {

	private static final ProfileReference APPROVER = new ProfileReference(UUID.randomUUID(), "Financial Manager");

	@Test
	@DisplayName("Configures an alcada with a threshold value only")
	void shouldConfigureAlcadaWithThresholdValueOnly() {
		final ApprovalAlcada alcada = ApprovalAlcada.configure(ApprovalModule.PURCHASING, new BigDecimal("5000.00"), null,
				APPROVER);

		assertThat(alcada.getId()).isNotNull();
		assertThat(alcada.getModule()).isEqualTo(ApprovalModule.PURCHASING);
		assertThat(alcada.getThresholdValue()).isEqualByComparingTo("5000.00");
		assertThat(alcada.getThresholdDiscountPercent()).isNull();
		assertThat(alcada.getApproverProfileId()).isEqualTo(APPROVER.id());
		assertThat(alcada.getConfiguredAt()).isNotNull();
	}

	@Test
	@DisplayName("Configures an alcada with a discount percent only")
	void shouldConfigureAlcadaWithDiscountPercentOnly() {
		final ApprovalAlcada alcada = ApprovalAlcada.configure(ApprovalModule.SALES, null, new BigDecimal("15.00"),
				APPROVER);

		assertThat(alcada.getThresholdValue()).isNull();
		assertThat(alcada.getThresholdDiscountPercent()).isEqualByComparingTo("15.00");
	}

	@Test
	@DisplayName("Rejects a configuration without any threshold")
	void shouldRejectConfigurationWithoutAnyThreshold() {
		assertThatThrownBy(() -> ApprovalAlcada.configure(ApprovalModule.FINANCE, null, null, APPROVER))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("thresholdValue or thresholdDiscountPercent");
	}

	@Test
	@DisplayName("Rejects negative thresholds")
	void shouldRejectNegativeThresholds() {
		assertThatThrownBy(() -> ApprovalAlcada.configure(ApprovalModule.FINANCE, new BigDecimal("-1"), null, APPROVER))
				.isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(
				() -> ApprovalAlcada.configure(ApprovalModule.FINANCE, null, new BigDecimal("-1"), APPROVER))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Reconfigures in place without changing id or module")
	void shouldReconfigureInPlaceWithoutChangingIdOrModule() {
		final ApprovalAlcada alcada = ApprovalAlcada.configure(ApprovalModule.FINANCE, new BigDecimal("1000.00"), null,
				APPROVER);
		final UUID originalId = alcada.getId();
		final Instant originalConfiguredAt = alcada.getConfiguredAt();
		final ProfileReference newApprover = new ProfileReference(UUID.randomUUID(), "CFO");

		alcada.reconfigure(new BigDecimal("2000.00"), new BigDecimal("10.00"), newApprover);

		assertThat(alcada.getId()).isEqualTo(originalId);
		assertThat(alcada.getModule()).isEqualTo(ApprovalModule.FINANCE);
		assertThat(alcada.getThresholdValue()).isEqualByComparingTo("2000.00");
		assertThat(alcada.getThresholdDiscountPercent()).isEqualByComparingTo("10.00");
		assertThat(alcada.getApproverProfileId()).isEqualTo(newApprover.id());
		assertThat(alcada.getConfiguredAt()).isAfterOrEqualTo(originalConfiguredAt);
	}
}
