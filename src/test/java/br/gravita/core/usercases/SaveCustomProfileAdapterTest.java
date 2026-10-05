package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PermissionAction;
import br.gravita.core.domain.PermissionDomain;
import br.gravita.core.domain.ProfileDomain;
import br.gravita.core.domain.exceptions.DuplicateResourceException;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveCustomProfileAdapterTest {

	@Mock
	private ProfileRepositoryPort profileRepositoryPort;

	@DisplayName("Creates a custom profile when its name is unique")
	@Test
	void shouldCreateCustomProfileWithUniqueName() {
		final SaveCustomProfileAdapter adapter = new SaveCustomProfileAdapter(profileRepositoryPort);
		final UUID id = UUID.randomUUID();
		final List<PermissionDomain> permissions = List.of(
				PermissionDomain.builder().module("sales").screen("orders").action(PermissionAction.VIEW).build());
		final ProfileDomain command = ProfileDomain.builder().id(id).name("Sales Read-Only").permissions(permissions).build();
		when(profileRepositoryPort.findByName("Sales Read-Only")).thenReturn(Optional.empty());
		when(profileRepositoryPort.save(command)).thenReturn(command);

		final ProfileDomain result = adapter.execute(new Context(command));

		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getName()).isEqualTo("Sales Read-Only");
		assertThat(result.getPermissions()).isEqualTo(permissions);
	}

	@DisplayName("Fails when the custom profile name is already in use")
	@Test
	void shouldFailWhenNameAlreadyInUse() {
		final SaveCustomProfileAdapter adapter = new SaveCustomProfileAdapter(profileRepositoryPort);
		final UUID id = UUID.randomUUID();
		final UUID otherId = UUID.randomUUID();
		final ProfileDomain command = ProfileDomain.builder().id(id).name("Financial").permissions(List.of()).build();
		when(profileRepositoryPort.findByName("Financial"))
				.thenReturn(Optional.of(ProfileDomain.builder().id(otherId).name("Financial").build()));

		assertThatThrownBy(() -> adapter.execute(new Context(command)))
				.isInstanceOf(DuplicateResourceException.class);
		verify(profileRepositoryPort, never()).save(org.mockito.ArgumentMatchers.any());
	}
}
