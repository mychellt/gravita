package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.IbgeMunicipalityDomain;
import br.gravita.core.ports.outbound.persistence.IbgeMunicipalityRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportIbgeMunicipalitiesAdapterTest {

	@Mock
	private IbgeMunicipalityRepositoryPort ibgeMunicipalityRepositoryPort;

	@Test
	void shouldAssignNewIdForUnseenMunicipality() {
		ImportIbgeMunicipalitiesAdapter adapter = new ImportIbgeMunicipalitiesAdapter(ibgeMunicipalityRepositoryPort);
		IbgeMunicipalityDomain incoming = IbgeMunicipalityDomain.builder().ibgeCode("3550308").name("São Paulo").stateCode("SP").build();
		when(ibgeMunicipalityRepositoryPort.findByIbgeCode("3550308")).thenReturn(Optional.empty());
		when(ibgeMunicipalityRepositoryPort.saveAll(List.of(incoming))).thenAnswer(invocation -> invocation.getArgument(0));

		adapter.execute(new Context(List.of(incoming)));

		assertThat(incoming.getId()).isNotNull();
	}

	@Test
	void shouldUpsertByIbgeCodeSoReimportIsIdempotent() {
		ImportIbgeMunicipalitiesAdapter adapter = new ImportIbgeMunicipalitiesAdapter(ibgeMunicipalityRepositoryPort);
		UUID existingId = UUID.randomUUID();
		IbgeMunicipalityDomain incoming = IbgeMunicipalityDomain.builder().ibgeCode("3550308").name("São Paulo").stateCode("SP").build();
		when(ibgeMunicipalityRepositoryPort.findByIbgeCode("3550308"))
				.thenReturn(Optional.of(IbgeMunicipalityDomain.builder().id(existingId).ibgeCode("3550308").build()));
		when(ibgeMunicipalityRepositoryPort.saveAll(List.of(incoming))).thenAnswer(invocation -> invocation.getArgument(0));

		adapter.execute(new Context(List.of(incoming)));

		assertThat(incoming.getId()).isEqualTo(existingId);
		ArgumentCaptor<List<IbgeMunicipalityDomain>> captor = ArgumentCaptor.forClass(List.class);
		verify(ibgeMunicipalityRepositoryPort).saveAll(captor.capture());
		assertThat(captor.getValue()).containsExactly(incoming);
	}
}
