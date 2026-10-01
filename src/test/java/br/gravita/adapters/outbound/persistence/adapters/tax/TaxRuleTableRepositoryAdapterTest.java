package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.TaxRateRuleJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.tax.TaxRateRuleJpaRepository;
import br.gravita.core.domain.tax.TaxRegime;
import br.gravita.core.ports.outbound.persistence.tax.TaxRateQuery;
import br.gravita.core.domain.tax.TaxRateRule;
import br.gravita.core.domain.tax.TaxType;
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
class TaxRuleTableRepositoryAdapterTest {

	@Mock
	private TaxRateRuleJpaRepository repository;

	@InjectMocks
	private TaxRuleTableRepositoryAdapter adapter;

	@Test
	@DisplayName("Finds the rules matching every dimension of the query and maps them to the domain")
	void findsTheRulesMatchingEveryDimensionOfTheQuery() {
		final TaxRateQuery query = new TaxRateQuery("85171231", "SP", "SP", TaxRegime.SIMPLES_NACIONAL, "VENDA_PDV");
		when(repository.findByNcmAndOriginStateAndDestinationStateAndRegimeAndOperationType("85171231", "SP", "SP",
				TaxRegime.SIMPLES_NACIONAL, "VENDA_PDV")).thenReturn(List.of(buildEntity()));

		final List<TaxRateRule> rates = adapter.findApplicableRates(query);

		assertThat(rates).hasSize(1);
		assertThat(rates.get(0).ncm()).isEqualTo("85171231");
		assertThat(rates.get(0).taxType()).isEqualTo(TaxType.ICMS);
		assertThat(rates.get(0).ratePercentage()).isEqualByComparingTo("18.0000");
		verify(repository).findByNcmAndOriginStateAndDestinationStateAndRegimeAndOperationType("85171231", "SP", "SP",
				TaxRegime.SIMPLES_NACIONAL, "VENDA_PDV");
	}

	@Test
	@DisplayName("Returns an empty list when no rule matches the query")
	void returnsAnEmptyListWhenNoRuleMatches() {
		final TaxRateQuery query = new TaxRateQuery("00000000", "SP", "SP", TaxRegime.SIMPLES_NACIONAL, "VENDA_PDV");
		when(repository.findByNcmAndOriginStateAndDestinationStateAndRegimeAndOperationType("00000000", "SP", "SP",
				TaxRegime.SIMPLES_NACIONAL, "VENDA_PDV")).thenReturn(List.of());

		assertThat(adapter.findApplicableRates(query)).isEmpty();
	}

	private TaxRateRuleJpaEntity buildEntity() {
		return TaxRateRuleJpaEntity.builder()
				.id(UUID.randomUUID())
				.ncm("85171231")
				.originState("SP")
				.destinationState("SP")
				.regime(TaxRegime.SIMPLES_NACIONAL)
				.operationType("VENDA_PDV")
				.taxType(TaxType.ICMS)
				.ratePercentage(new BigDecimal("18.0000"))
				.baseReductionPercentage(BigDecimal.ZERO)
				.mvaPercentage(BigDecimal.ZERO)
				.build();
	}
}
