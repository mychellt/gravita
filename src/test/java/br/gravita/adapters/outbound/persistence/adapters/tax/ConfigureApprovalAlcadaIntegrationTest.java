package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.ProfileJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.ApprovalAlcadaPersistenceMapperImpl;
import br.gravita.core.domain.system.ApprovalAlcada;
import br.gravita.core.domain.system.ApprovalModule;
import br.gravita.core.domain.system.UnknownApprovalModuleException;
import br.gravita.core.domain.system.UnknownProfileException;
import br.gravita.core.usercases.system.ConfigureApprovalAlcadaCommand;
import br.gravita.core.usercases.tax.ConfigureApprovalAlcadaService;
import org.junit.jupiter.api.DisplayName;
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
	@DisplayName("Updates the single row when the same module is reconfigured instead of creating a second one")
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
	@DisplayName("Accepts a configuration that sets only the discount percent threshold")
	void configuringWithOnlyThresholdDiscountPercentSucceeds() {
		UUID approver = persistProfile("Sales Manager");

		service().execute(new ConfigureApprovalAlcadaCommand("sales", null, new BigDecimal("15.00"), approver));
		flushAndClear();

		ApprovalAlcada persisted = approvalAlcadaRepositoryAdapter.findByModule(ApprovalModule.SALES).orElseThrow();
		assertThat(persisted.getThresholdDiscountPercent()).isEqualByComparingTo("15.00");
		assertThat(persisted.getThresholdValue()).isNull();
	}

	@Test
	@DisplayName("Rejects a configuration that sets neither threshold")
	void configuringWithNeitherThresholdSetIsRejected() {
		UUID approver = persistProfile("Finance Manager");

		assertThatThrownBy(() -> service().execute(new ConfigureApprovalAlcadaCommand("finance", null, null, approver)))
				.hasMessageContaining("At least one of thresholdValue or thresholdDiscountPercent must be set");

		assertThat(approvalAlcadaRepositoryAdapter.findByModule(ApprovalModule.FINANCE)).isEmpty();
	}

	@Test
	@DisplayName("Rejects an invalid module, naming the module in the error")
	void anInvalidModuleIsRejectedNamingTheModule() {
		UUID approver = persistProfile("Someone");

		assertThatThrownBy(() -> service().execute(
				new ConfigureApprovalAlcadaCommand("logistics", new BigDecimal("100"), null, approver)))
				.isInstanceOf(UnknownApprovalModuleException.class)
				.hasMessageContaining("logistics");
	}

	@Test
	@DisplayName("Rejects a non-existent approver profile")
	void aNonExistentApproverProfileIsRejected() {
		UUID unknownProfile = UUID.randomUUID();

		assertThatThrownBy(() -> service().execute(
				new ConfigureApprovalAlcadaCommand("purchasing", new BigDecimal("100"), null, unknownProfile)))
				.isInstanceOf(UnknownProfileException.class);

		assertThat(approvalAlcadaRepositoryAdapter.findByModule(ApprovalModule.PURCHASING)).isEmpty();
	}

	@Test
	@DisplayName("Accepts a custom non-standard profile like any other existing profile")
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
