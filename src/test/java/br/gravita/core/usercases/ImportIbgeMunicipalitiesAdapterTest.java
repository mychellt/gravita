package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.IbgeMunicipalityDomain;
import br.gravita.core.ports.outbound.persistence.IbgeMunicipalityRepositoryPort;
import org.junit.jupiter.api.DisplayName;
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

	@DisplayName("Importing a municipality not seen before assigns it a new id")
	@Test
	void shouldAssignNewIdForUnseenMunicipality() {
		final ImportIbgeMunicipalitiesAdapter adapter = new ImportIbgeMunicipalitiesAdapter(ibgeMunicipalityRepositoryPort);
		final IbgeMunicipalityDomain incoming = IbgeMunicipalityDomain.builder().ibgeCode("3550308").name("São Paulo").stateCode("SP").build();
		when(ibgeMunicipalityRepositoryPort.findByIbgeCode("3550308")).thenReturn(Optional.empty());
		when(ibgeMunicipalityRepositoryPort.saveAll(List.of(incoming))).thenAnswer(invocation -> invocation.getArgument(0));

		adapter.execute(new Context(List.of(incoming)));

		assertThat(incoming.getId()).isNotNull();
	}

	@DisplayName("Upserts by IBGE code so that re-importing does not duplicate municipalities")
	@Test
	void shouldUpsertByIbgeCodeSoReimportIsIdempotent() {
		final ImportIbgeMunicipalitiesAdapter adapter = new ImportIbgeMunicipalitiesAdapter(ibgeMunicipalityRepositoryPort);
		final UUID existingId = UUID.randomUUID();
		final IbgeMunicipalityDomain incoming = IbgeMunicipalityDomain.builder().ibgeCode("3550308").name("São Paulo").stateCode("SP").build();
		when(ibgeMunicipalityRepositoryPort.findByIbgeCode("3550308"))
				.thenReturn(Optional.of(IbgeMunicipalityDomain.builder().id(existingId).ibgeCode("3550308").build()));
		when(ibgeMunicipalityRepositoryPort.saveAll(List.of(incoming))).thenAnswer(invocation -> invocation.getArgument(0));

		adapter.execute(new Context(List.of(incoming)));

		assertThat(incoming.getId()).isEqualTo(existingId);
		final ArgumentCaptor<List<IbgeMunicipalityDomain>> captor = ArgumentCaptor.forClass(List.class);
		verify(ibgeMunicipalityRepositoryPort).saveAll(captor.capture());
		assertThat(captor.getValue()).containsExactly(incoming);
	}
}
