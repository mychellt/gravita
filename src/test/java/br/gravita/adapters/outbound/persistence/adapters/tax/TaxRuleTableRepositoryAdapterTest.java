package br.gravita.adapters.outbound.persistence.adapters.tax;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.adapters.outbound.persistence.entities.tax.TaxRateRuleJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.tax.TaxRateRuleJpaRepository;
import br.gravita.core.domain.tax.TaxRateRule;
import br.gravita.core.domain.tax.TaxRegime;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.ports.outbound.persistence.tax.TaxRateQuery;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(TaxRuleTableRepositoryAdapter.class)
class TaxRuleTableRepositoryAdapterTest {

	@Autowired
	private TaxRuleTableRepositoryAdapter repositoryAdapter;

	@Autowired
	private TaxRateRuleJpaRepository jpaRepository;

	@Test
	@DisplayName("Finds only the rules matching every dimension of the query")
	void findsOnlyTheRulesMatchingEveryDimensionOfTheQuery() {
		jpaRepository.save(matchingRule());
		jpaRepository.save(ruleFor("85171231", "SP", "RJ"));

		List<TaxRateRule> rates = repositoryAdapter
				.findApplicableRates(new TaxRateQuery("85171231", "SP", "SP", TaxRegime.SIMPLES_NACIONAL, "VENDA_PDV"));

		assertThat(rates).hasSize(1);
		assertThat(rates.get(0).ratePercentage()).isEqualByComparingTo("18.0000");
	}

	@Test
	@DisplayName("Returns an empty list when no rule matches the query")
	void returnsAnEmptyListWhenNoRuleMatches() {
		List<TaxRateRule> rates = repositoryAdapter
				.findApplicableRates(new TaxRateQuery("00000000", "SP", "SP", TaxRegime.SIMPLES_NACIONAL, "VENDA_PDV"));

		assertThat(rates).isEmpty();
	}

	private TaxRateRuleJpaEntity matchingRule() {
		return ruleFor("85171231", "SP", "SP");
	}

	private TaxRateRuleJpaEntity ruleFor(String ncm, String originState, String destinationState) {
		TaxRateRuleJpaEntity entity = TaxRateRuleJpaEntity.builder()
				.id(UUID.randomUUID())
				.ncm(ncm)
				.originState(originState)
				.destinationState(destinationState)
				.regime(TaxRegime.SIMPLES_NACIONAL)
				.operationType("VENDA_PDV")
				.taxType(TaxType.ICMS)
				.ratePercentage(new BigDecimal("18.0000"))
				.baseReductionPercentage(BigDecimal.ZERO)
				.mvaPercentage(BigDecimal.ZERO)
				.build();
		entity.setNew(true);
		return entity;
	}
}
