package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.tax.Cfop;
import br.gravita.core.domain.tax.CfopRegistryEntry;
import br.gravita.core.domain.tax.NaturezaOperacao;
import br.gravita.core.ports.outbound.tax.CfopRegistryPort;
import br.gravita.core.usercases.tax.ListCfopRegistryService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListCfopRegistryServiceTest {

	@Mock
	private CfopRegistryPort cfopRegistryPort;

	@InjectMocks
	private ListCfopRegistryService service;

	@Test
	@DisplayName("Returns what the CFOP registry port holds")
	void returnsTheRegistryEntries() {
		final List<CfopRegistryEntry> entries = List.of(
				new CfopRegistryEntry(NaturezaOperacao.VENDA, new Cfop("5102"), "Venda de mercadoria"));
		when(cfopRegistryPort.findAll()).thenReturn(entries);

		assertThat(service.execute(new Context())).isEqualTo(entries);
	}
}
