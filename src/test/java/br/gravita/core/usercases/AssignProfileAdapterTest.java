package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PermissionAction;
import br.gravita.core.domain.PermissionDomain;
import br.gravita.core.domain.ProfileDomain;
import br.gravita.core.domain.exceptions.DuplicateResourceException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.persistence.ProfileRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignProfileAdapterTest {

	@Mock
	private ProfileRepositoryPort profileRepositoryPort;

	@Test
	void shouldReplacePermissionsWhenProfileExists() {
		AssignProfileAdapter adapter = new AssignProfileAdapter(profileRepositoryPort);
		UUID id = UUID.randomUUID();
		ProfileDomain existing = ProfileDomain.builder().id(id).name("Financial")
				.permissions(List.of(PermissionDomain.builder().module("finance").screen("invoices")
						.action(PermissionAction.VIEW).build()))
				.build();
		List<PermissionDomain> newPermissions = List.of(
				PermissionDomain.builder().module("finance").screen("invoices").action(PermissionAction.EDIT).build(),
				PermissionDomain.builder().module("finance").screen("reports").action(PermissionAction.EXPORT).build());
		ProfileDomain command = ProfileDomain.builder().id(id).permissions(newPermissions).build();
		when(profileRepositoryPort.findById(id)).thenReturn(Optional.of(existing));
		when(profileRepositoryPort.save(existing)).thenReturn(existing);

		ProfileDomain result = adapter.execute(new Context(command));

		ArgumentCaptor<ProfileDomain> captor = ArgumentCaptor.forClass(ProfileDomain.class);
		verify(profileRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getId()).isEqualTo(id);
		assertThat(captor.getValue().getPermissions()).isEqualTo(newPermissions);
		assertThat(result.getName()).isEqualTo("Financial");
	}

	@Test
	void shouldFailWhenProfileNotFound() {
		AssignProfileAdapter adapter = new AssignProfileAdapter(profileRepositoryPort);
		UUID id = UUID.randomUUID();
		ProfileDomain command = ProfileDomain.builder().id(id).permissions(List.of()).build();
		when(profileRepositoryPort.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(command)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void shouldCreateCustomProfileWhenIdNotFoundAndNameProvided() {
		AssignProfileAdapter adapter = new AssignProfileAdapter(profileRepositoryPort);
		UUID id = UUID.randomUUID();
		List<PermissionDomain> permissions = List.of(
				PermissionDomain.builder().module("sales").screen("orders").action(PermissionAction.VIEW).build());
		ProfileDomain command = ProfileDomain.builder().id(id).name("Sales Read-Only").permissions(permissions).build();
		when(profileRepositoryPort.findById(id)).thenReturn(Optional.empty());
		when(profileRepositoryPort.findByName("Sales Read-Only")).thenReturn(Optional.empty());
		when(profileRepositoryPort.save(command)).thenReturn(command);

		ProfileDomain result = adapter.execute(new Context(command));

		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getName()).isEqualTo("Sales Read-Only");
		assertThat(result.getPermissions()).isEqualTo(permissions);
	}

	@Test
	void shouldFailToCreateCustomProfileWhenNameAlreadyInUse() {
		AssignProfileAdapter adapter = new AssignProfileAdapter(profileRepositoryPort);
		UUID id = UUID.randomUUID();
		UUID otherId = UUID.randomUUID();
		ProfileDomain command = ProfileDomain.builder().id(id).name("Financial").permissions(List.of()).build();
		when(profileRepositoryPort.findById(id)).thenReturn(Optional.empty());
		when(profileRepositoryPort.findByName("Financial"))
				.thenReturn(Optional.of(ProfileDomain.builder().id(otherId).name("Financial").build()));

		assertThatThrownBy(() -> adapter.execute(new Context(command)))
				.isInstanceOf(DuplicateResourceException.class);
	}
}
