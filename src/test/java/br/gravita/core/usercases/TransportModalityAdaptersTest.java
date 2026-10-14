package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.TransportModalityDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.DuplicateResourceException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.outbound.persistence.TransportModalityRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransportModalityAdaptersTest {

	@Mock
	private TransportModalityRepositoryPort repository;

	private static TransportModalityDomain modality(final UUID id, final String name, final String code, final Boolean active) {
		return TransportModalityDomain.builder().id(id).name(name).code(code).active(active).build();
	}

	@Test
	@DisplayName("Creates an active modality with a generated id and a normalised code")
	void shouldCreateActiveModality() {
		when(repository.findActiveByCode("FOB")).thenReturn(Optional.empty());
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final TransportModalityDomain created = new CreateTransportModalityAdapter(repository)
				.execute(new Context(modality(null, "  FOB — Por conta do destinatário ", " fob ", null)));

		assertThat(created.getId()).isNotNull();
		assertThat(created.getCode()).isEqualTo("FOB");
		assertThat(created.getName()).isEqualTo("FOB — Por conta do destinatário");
		assertThat(created.getActive()).isTrue();
	}

	@Test
	@DisplayName("Rejects codes outside CIF, FOB and NONE — the registry only holds values the NF-e can resolve")
	void shouldRejectUnsupportedCode() {
		final CreateTransportModalityAdapter adapter = new CreateTransportModalityAdapter(repository);

		assertThatThrownBy(() -> adapter.execute(new Context(modality(null, "Por conta de terceiros", "TERCEIROS", null))))
				.isInstanceOf(BusinessRuleException.class);
		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("Rejects a blank name")
	void shouldRejectBlankName() {
		final CreateTransportModalityAdapter adapter = new CreateTransportModalityAdapter(repository);

		assertThatThrownBy(() -> adapter.execute(new Context(modality(null, "  ", "CIF", null))))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Rejects a second active modality with the same code")
	void shouldRejectDuplicateActiveCode() {
		when(repository.findActiveByCode("CIF")).thenReturn(Optional.of(modality(UUID.randomUUID(), "CIF", "CIF", true)));

		assertThatThrownBy(() -> new CreateTransportModalityAdapter(repository)
				.execute(new Context(modality(null, "CIF outra", "CIF", true))))
				.isInstanceOf(DuplicateResourceException.class);
		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("An inactive modality can be created for a code that already has an active one")
	void shouldAllowInactiveDuplicate() {
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final TransportModalityDomain created = new CreateTransportModalityAdapter(repository)
				.execute(new Context(modality(null, "CIF antiga", "CIF", false)));

		assertThat(created.getActive()).isFalse();
		verify(repository, never()).findActiveByCode(any());
	}

	@Test
	@DisplayName("Updates name and code, keeping the stored active flag and audit timestamps when active is omitted")
	void shouldUpdateKeepingActiveFlag() {
		final UUID id = UUID.randomUUID();
		final TransportModalityDomain stored = modality(id, "CIF", "CIF", false);
		when(repository.get(id)).thenReturn(Optional.of(stored));
		when(repository.save(stored)).thenReturn(stored);

		final TransportModalityDomain updated = new UpdateTransportModalityAdapter(repository)
				.execute(new Context(modality(id, "CIF — Por conta do emitente", "cif", null)));

		assertThat(updated.getName()).isEqualTo("CIF — Por conta do emitente");
		assertThat(updated.getActive()).isFalse();
		verify(repository).save(stored);
	}

	@Test
	@DisplayName("Reactivating a modality is refused while another active one holds the same code")
	void shouldRejectReactivationClash() {
		final UUID id = UUID.randomUUID();
		when(repository.get(id)).thenReturn(Optional.of(modality(id, "CIF antiga", "CIF", false)));
		when(repository.findActiveByCode("CIF")).thenReturn(Optional.of(modality(UUID.randomUUID(), "CIF", "CIF", true)));

		assertThatThrownBy(() -> new UpdateTransportModalityAdapter(repository)
				.execute(new Context(modality(id, "CIF antiga", "CIF", true))))
				.isInstanceOf(DuplicateResourceException.class);
		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("Editing an active modality does not clash with itself")
	void shouldNotClashWithItself() {
		final UUID id = UUID.randomUUID();
		final TransportModalityDomain stored = modality(id, "CIF", "CIF", true);
		when(repository.get(id)).thenReturn(Optional.of(stored));
		when(repository.findActiveByCode("CIF")).thenReturn(Optional.of(stored));
		when(repository.save(stored)).thenReturn(stored);

		assertThat(new UpdateTransportModalityAdapter(repository).execute(new Context(modality(id, "CIF renomeada", "CIF", null))).getName())
				.isEqualTo("CIF renomeada");
	}

	@Test
	@DisplayName("Update of an unknown modality fails with not found")
	void shouldFailUpdatingUnknownModality() {
		final UUID id = UUID.randomUUID();
		when(repository.get(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> new UpdateTransportModalityAdapter(repository).execute(new Context(modality(id, "x", "CIF", null))))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("Delete only deactivates the row — it is saved back inactive, never removed")
	void shouldSoftDelete() {
		final UUID id = UUID.randomUUID();
		final TransportModalityDomain stored = modality(id, "CIF", "CIF", true);
		when(repository.get(id)).thenReturn(Optional.of(stored));

		new DeleteTransportModalityAdapter(repository).execute(new Context(id));

		assertThat(stored.getActive()).isFalse();
		verify(repository).save(stored);
	}

	@Test
	@DisplayName("Deleting an already inactive modality is a no-op")
	void shouldIgnoreDeletingInactiveModality() {
		final UUID id = UUID.randomUUID();
		when(repository.get(id)).thenReturn(Optional.of(modality(id, "CIF", "CIF", false)));

		new DeleteTransportModalityAdapter(repository).execute(new Context(id));

		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("Delete of an unknown modality fails with not found")
	void shouldFailDeletingUnknownModality() {
		final UUID id = UUID.randomUUID();
		when(repository.get(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> new DeleteTransportModalityAdapter(repository).execute(new Context(id)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("Lists only active modalities unless includeInactive is requested")
	void shouldListActiveOrAll() {
		final TransportModalityDomain active = modality(UUID.randomUUID(), "CIF", "CIF", true);
		final TransportModalityDomain inactive = modality(UUID.randomUUID(), "FOB", "FOB", false);
		when(repository.findAllActive()).thenReturn(List.of(active));
		when(repository.findAll()).thenReturn(List.of(active, inactive));
		final ListTransportModalitiesAdapter adapter = new ListTransportModalitiesAdapter(repository);

		assertThat(adapter.execute(new Context(false))).containsExactly(active);
		assertThat(adapter.execute(new Context())).containsExactly(active);
		assertThat(adapter.execute(new Context(true))).containsExactly(active, inactive);
	}

	@Test
	@DisplayName("Find returns the modality or fails with not found")
	void shouldFind() {
		final UUID id = UUID.randomUUID();
		final TransportModalityDomain stored = modality(id, "CIF", "CIF", true);
		when(repository.get(id)).thenReturn(Optional.of(stored));
		final FindTransportModalityAdapter adapter = new FindTransportModalityAdapter(repository);

		assertThat(adapter.execute(new Context(id))).isSameAs(stored);
		final UUID missing = UUID.randomUUID();
		when(repository.get(missing)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> adapter.execute(new Context(missing))).isInstanceOf(ResourceNotFoundException.class);
	}
}
