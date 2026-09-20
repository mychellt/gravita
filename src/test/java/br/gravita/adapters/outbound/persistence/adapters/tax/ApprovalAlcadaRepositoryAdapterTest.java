package br.gravita.system.adapter.out.persistence;

import br.gravita.adapters.outbound.persistence.adapters.tax.ApprovalAlcadaRepositoryAdapter;
import br.gravita.adapters.outbound.persistence.entities.ProfileJpaEntity;
import br.gravita.core.domain.system.ApprovalAlcada;
import br.gravita.core.domain.system.ApprovalModule;
import br.gravita.core.domain.system.ProfileReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(ApprovalAlcadaRepositoryAdapter.class)
class ApprovalAlcadaRepositoryAdapterTest {

	@Autowired
	private ApprovalAlcadaRepositoryAdapter repositoryAdapter;

	@Autowired
	private TestEntityManager entityManager;

	private ProfileReference persistApprover(String name) {
		UUID id = UUID.randomUUID();
		entityManager.persist(ProfileJpaEntity.builder().id(id).name(name).permissions(List.of()).build());
		return new ProfileReference(id, name);
	}

	@Test
	void shouldSaveAndRetrieveAlcadaByModule() {
		ProfileReference approver = persistApprover("Purchasing Manager");
		ApprovalAlcada alcada = ApprovalAlcada.configure(ApprovalModule.PURCHASING, new BigDecimal("5000.00"), null,
				approver);

		repositoryAdapter.save(alcada);

		Optional<ApprovalAlcada> found = repositoryAdapter.findByModule(ApprovalModule.PURCHASING);
		assertThat(found).isPresent();
		assertThat(found.get().getThresholdValue()).isEqualByComparingTo("5000.00");
		assertThat(found.get().getApproverProfileId()).isEqualTo(approver.id());
	}

	@Test
	void shouldReturnEmptyWhenModuleHasNoAlcadaConfigured() {
		assertThat(repositoryAdapter.findByModule(ApprovalModule.SALES)).isEmpty();
	}

	@Test
	void shouldKeepEachModuleConfigurationIndependent() {
		ProfileReference approver = persistApprover("Ops Manager");
		repositoryAdapter.save(ApprovalAlcada.configure(ApprovalModule.PURCHASING, new BigDecimal("1000.00"), null,
				approver));
		repositoryAdapter.save(ApprovalAlcada.configure(ApprovalModule.SALES, null, new BigDecimal("10.00"),
				approver));

		assertThat(repositoryAdapter.findByModule(ApprovalModule.PURCHASING)).get()
				.extracting(ApprovalAlcada::getThresholdValue).isEqualTo(new BigDecimal("1000.00"));
		assertThat(repositoryAdapter.findByModule(ApprovalModule.SALES)).get()
				.extracting(ApprovalAlcada::getThresholdDiscountPercent).isEqualTo(new BigDecimal("10.00"));
		assertThat(repositoryAdapter.findByModule(ApprovalModule.FINANCE)).isEmpty();
	}

	@Test
	void reconfiguringAndSavingAgainImmediatelyReflectsTheNewValueOnNextRead() {
		ProfileReference approver = persistApprover("Finance Manager");
		ApprovalAlcada alcada = ApprovalAlcada.configure(ApprovalModule.FINANCE, new BigDecimal("2000.00"), null,
				approver);
		repositoryAdapter.save(alcada);

		ProfileReference newApprover = persistApprover("CFO");
		alcada.reconfigure(new BigDecimal("3000.00"), null, newApprover);
		repositoryAdapter.save(alcada);

		Optional<ApprovalAlcada> found = repositoryAdapter.findByModule(ApprovalModule.FINANCE);
		assertThat(found).isPresent();
		assertThat(found.get().getThresholdValue()).isEqualByComparingTo("3000.00");
		assertThat(found.get().getApproverProfileId()).isEqualTo(newApprover.id());
	}
}
