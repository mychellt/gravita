package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.ProfileJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.ApprovalAlcadaPersistenceMapperImpl;
import br.gravita.core.domain.system.ApprovalAlcada;
import br.gravita.core.domain.system.ApprovalModule;
import br.gravita.core.domain.system.UnknownApprovalModuleException;
import br.gravita.core.domain.system.UnknownProfileException;
import br.gravita.core.usercases.system.ConfigureApprovalAlcadaCommand;
import br.gravita.core.usercases.tax.ConfigureApprovalAlcadaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * GRA-49: independent QA verification of GRA-38's ConfigureApprovalAlcadaUseCase
 * (PUT /api/system/alcadas/{module}). ConfigureApprovalAlcadaServiceTest mocks
 * both ports, so it can't prove real behaviour. This drives the real
 * ConfigureApprovalAlcadaService against real H2-backed repositories
 * (@DataJpaTest) for both the alcada table and the profile lookup, covering
 * what the mocked test cannot: exactly-one-config-per-module is enforced by
 * an update-in-place (not a second row), and that a persisted profile is
 * accepted regardless of its name (no allowlist beyond "does it exist").
 *
 * A full @SpringBootTest/HTTP-level equivalent could not be used because the
 * application context currently fails to start (TotpVerificationAdapter has
 * ambiguous constructors, tracked separately as GRA-54), so this exercises
 * the real use case + repositories + database directly instead.
 */
@DataJpaTest
@Import({ApprovalAlcadaRepositoryAdapter.class, ApprovalAlcadaPersistenceMapperImpl.class,
		ProfileLookupRepositoryAdapter.class})
class ConfigureApprovalAlcadaIntegrationTest {

	@Autowired
	private ApprovalAlcadaRepositoryAdapter approvalAlcadaRepositoryAdapter;

	@Autowired
	private ProfileLookupRepositoryAdapter profileLookupRepositoryAdapter;

	@Autowired
	private TestEntityManager entityManager;

	private ConfigureApprovalAlcadaService service() {
		return new ConfigureApprovalAlcadaService(approvalAlcadaRepositoryAdapter, profileLookupRepositoryAdapter);
	}

	private UUID persistProfile(String name) {
		UUID id = UUID.randomUUID();
		entityManager.persist(ProfileJpaEntity.builder().id(id).name(name).permissions(List.of()).build());
		return id;
	}

	@Test
	void reconfiguringTheSameModuleUpdatesTheSingleRowInsteadOfCreatingASecondOne() {
		UUID approver = persistProfile("Purchasing Manager");
		ConfigureApprovalAlcadaService service = service();

		service.execute(new ConfigureApprovalAlcadaCommand("purchasing", new BigDecimal("5000.00"), null, approver));
		flushAndClear();
		service.execute(new ConfigureApprovalAlcadaCommand("purchasing", new BigDecimal("8000.00"), null, approver));
		flushAndClear();

		ApprovalAlcada persisted = approvalAlcadaRepositoryAdapter.findByModule(ApprovalModule.PURCHASING).orElseThrow();
		assertThat(persisted.getThresholdValue()).isEqualByComparingTo("8000.00");
		assertThat(entityManager.getEntityManager()
				.createQuery("select count(a) from ApprovalAlcadaJpaEntity a where a.module = :m", Long.class)
				.setParameter("m", ApprovalModule.PURCHASING)
				.getSingleResult()).isEqualTo(1L);
	}

	@Test
	void configuringWithOnlyThresholdDiscountPercentSucceeds() {
		UUID approver = persistProfile("Sales Manager");

		service().execute(new ConfigureApprovalAlcadaCommand("sales", null, new BigDecimal("15.00"), approver));
		flushAndClear();

		ApprovalAlcada persisted = approvalAlcadaRepositoryAdapter.findByModule(ApprovalModule.SALES).orElseThrow();
		assertThat(persisted.getThresholdDiscountPercent()).isEqualByComparingTo("15.00");
		assertThat(persisted.getThresholdValue()).isNull();
	}

	@Test
	void configuringWithNeitherThresholdSetIsRejected() {
		UUID approver = persistProfile("Finance Manager");

		assertThatThrownBy(() -> service().execute(new ConfigureApprovalAlcadaCommand("finance", null, null, approver)))
				.hasMessageContaining("At least one of thresholdValue or thresholdDiscountPercent must be set");

		assertThat(approvalAlcadaRepositoryAdapter.findByModule(ApprovalModule.FINANCE)).isEmpty();
	}

	@Test
	void anInvalidModuleIsRejectedNamingTheModule() {
		UUID approver = persistProfile("Someone");

		assertThatThrownBy(() -> service().execute(
				new ConfigureApprovalAlcadaCommand("logistics", new BigDecimal("100"), null, approver)))
				.isInstanceOf(UnknownApprovalModuleException.class)
				.hasMessageContaining("logistics");
	}

	@Test
	void aNonExistentApproverProfileIsRejected() {
		UUID unknownProfile = UUID.randomUUID();

		assertThatThrownBy(() -> service().execute(
				new ConfigureApprovalAlcadaCommand("purchasing", new BigDecimal("100"), null, unknownProfile)))
				.isInstanceOf(UnknownProfileException.class);

		assertThat(approvalAlcadaRepositoryAdapter.findByModule(ApprovalModule.PURCHASING)).isEmpty();
	}

	@Test
	void aCustomNonStandardProfileIsAcceptedJustLikeAnyOtherExistingProfile() {
		UUID customProfile = persistProfile("Custom Regional Approver");

		service().execute(new ConfigureApprovalAlcadaCommand("purchasing", new BigDecimal("2500.00"), null, customProfile));
		flushAndClear();

		ApprovalAlcada persisted = approvalAlcadaRepositoryAdapter.findByModule(ApprovalModule.PURCHASING).orElseThrow();
		assertThat(persisted.getApproverProfileId()).isEqualTo(customProfile);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
