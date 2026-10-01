package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.ServiceTaxRuleJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.tax.ServiceTaxRuleJpaRepository;
import br.gravita.core.domain.tax.ServiceTaxRule;
import br.gravita.core.domain.tax.TaxRegime;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.domain.tax.WithholdingMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceTaxRuleRepositoryAdapterTest {

	@Mock
	private ServiceTaxRuleJpaRepository repository;

	@InjectMocks
	private ServiceTaxRuleRepositoryAdapter adapter;

	@Test
	@DisplayName("Finds the candidate rules for a service code and municipality and maps them to the domain")
	void findCandidatesMapsTheRulesToTheDomain() {
		final ServiceTaxRuleJpaEntity iss = ServiceTaxRuleJpaEntity.builder()
				.id(UUID.randomUUID())
				.serviceCode("01.05")
				.municipalityIbge("3550308")
				.regime(TaxRegime.LUCRO_PRESUMIDO)
				.taxType(TaxType.ISS)
				.ratePercentage(new BigDecimal("5.0000"))
				.withholding(WithholdingMode.TOMADOR_COMPANY)
				.build();
		final ServiceTaxRuleJpaEntity pis = ServiceTaxRuleJpaEntity.builder()
				.id(UUID.randomUUID())
				.serviceCode("01.05")
				.taxType(TaxType.PIS)
				.ratePercentage(new BigDecimal("0.6500"))
				.withholding(WithholdingMode.ALWAYS)
				.build();
		when(repository.findCandidates("01.05", "3550308")).thenReturn(List.of(iss, pis));

		final List<ServiceTaxRule> candidates = adapter.findCandidates("01.05", "3550308");

		assertThat(candidates).extracting(ServiceTaxRule::taxType).containsExactly(TaxType.ISS, TaxType.PIS);
		assertThat(candidates.get(0).municipalityIbgeCode()).isEqualTo("3550308");
		assertThat(candidates.get(0).regime()).isEqualTo(TaxRegime.LUCRO_PRESUMIDO);
		assertThat(candidates.get(0).ratePercentage()).isEqualByComparingTo("5.0000");
		assertThat(candidates.get(0).withholding()).isEqualTo(WithholdingMode.TOMADOR_COMPANY);
		assertThat(candidates.get(1).municipalityIbgeCode()).isNull();
		verify(repository).findCandidates("01.05", "3550308");
	}

	@Test
	@DisplayName("Returns an empty list when there are no candidate rules")
	void findCandidatesReturnsEmptyWhenThereAreNoRules() {
		when(repository.findCandidates("02.01", "3550308")).thenReturn(List.of());

		assertThat(adapter.findCandidates("02.01", "3550308")).isEmpty();
	}
}
