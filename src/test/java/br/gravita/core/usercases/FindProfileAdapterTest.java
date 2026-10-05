package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PermissionAction;
import br.gravita.core.domain.PermissionDomain;
import br.gravita.core.domain.ProfileDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.outbound.persistence.ProfileRepositoryPort;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindProfileAdapterTest {

	@Mock
	private ProfileRepositoryPort profileRepositoryPort;

	@DisplayName("Returns the profile when it exists")
	@Test
	void shouldReturnProfileWhenFound() {
		final FindProfileAdapter adapter = new FindProfileAdapter(profileRepositoryPort);
		final UUID id = UUID.randomUUID();
		final ProfileDomain profile = ProfileDomain.builder().id(id).name("Financial")
				.permissions(List.of(PermissionDomain.builder().module("finance").screen("invoices")
						.action(PermissionAction.VIEW).build()))
				.build();
		when(profileRepositoryPort.findById(id)).thenReturn(Optional.of(profile));

		final ProfileDomain found = adapter.execute(new Context(id));

		assertThat(found).isEqualTo(profile);
	}

	@DisplayName("Fails with not found when the profile does not exist")
	@Test
	void shouldFailWhenProfileNotFound() {
		final FindProfileAdapter adapter = new FindProfileAdapter(profileRepositoryPort);
		final UUID id = UUID.randomUUID();
		when(profileRepositoryPort.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(id))).isInstanceOf(ResourceNotFoundException.class);
	}
}
