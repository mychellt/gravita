package br.gravita.adapters.outbound.persistence.adapters.tax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.gravita.adapters.outbound.persistence.entities.tax.CfopRegistryJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.tax.CfopRegistryJpaRepository;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.tax.Cfop;
import br.gravita.core.domain.tax.CfopRegistryEntry;
import br.gravita.core.domain.tax.NaturezaOperacao;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CfopRegistryRepositoryAdapterTest {

	@Mock
	private CfopRegistryJpaRepository repository;

	@InjectMocks
	private CfopRegistryRepositoryAdapter adapter;

	private static CfopRegistryJpaEntity row(final NaturezaOperacao natureza, final String cfop, final String description) {
		return CfopRegistryJpaEntity.builder().id(UUID.randomUUID()).naturezaOperacao(natureza).cfop(cfop)
				.description(description).build();
	}

	@Test
	@DisplayName("Resolves the CFOP stored for a natureza da operação")
	void resolveReadsTheCfopFromTheTable() {
		when(repository.findByNaturezaOperacaoAndActiveTrue(NaturezaOperacao.DEVOLUCAO))
				.thenReturn(Optional.of(row(NaturezaOperacao.DEVOLUCAO, "5202", "Devolução")));

		assertThat(adapter.resolve(NaturezaOperacao.DEVOLUCAO)).isEqualTo(new Cfop("5202"));
	}

	@Test
	@DisplayName("Rejects a natureza da operação without an active registry row")
	void resolveFailsWhenNothingIsRegistered() {
		when(repository.findByNaturezaOperacaoAndActiveTrue(NaturezaOperacao.OUTRA)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.resolve(NaturezaOperacao.OUTRA))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("OUTRA");
	}

	@Test
	@DisplayName("Lists the active rows in NaturezaOperacao declaration order")
	void findAllListsRowsInEnumOrder() {
		when(repository.findAllByActiveTrue()).thenReturn(List.of(
				row(NaturezaOperacao.OUTRA, "5949", "Outra saída"),
				row(NaturezaOperacao.VENDA, "5102", "Venda de mercadoria"),
				row(NaturezaOperacao.DEVOLUCAO, "5202", "Devolução")));

		assertThat(adapter.findAll()).containsExactly(
				new CfopRegistryEntry(NaturezaOperacao.VENDA, new Cfop("5102"), "Venda de mercadoria"),
				new CfopRegistryEntry(NaturezaOperacao.DEVOLUCAO, new Cfop("5202"), "Devolução"),
				new CfopRegistryEntry(NaturezaOperacao.OUTRA, new Cfop("5949"), "Outra saída"));
	}
}
