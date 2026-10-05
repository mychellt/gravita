package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PermissionAction;
import br.gravita.core.domain.PermissionDomain;
import br.gravita.core.domain.ProfileDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.SaveCustomProfilePort;
import br.gravita.core.ports.outbound.persistence.ProfileRepositoryPort;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignProfileAdapterTest {

	@Mock
	private ProfileRepositoryPort profileRepositoryPort;

	@Mock
	private SaveCustomProfilePort saveCustomProfilePort;

	@DisplayName("Assigning to an existing profile replaces its permissions")
	@Test
	void shouldReplacePermissionsWhenProfileExists() {
		final AssignProfileAdapter adapter = new AssignProfileAdapter(profileRepositoryPort, saveCustomProfilePort);
		final UUID id = UUID.randomUUID();
		final ProfileDomain existing = ProfileDomain.builder().id(id).name("Financial")
				.permissions(List.of(PermissionDomain.builder().module("finance").screen("invoices")
						.action(PermissionAction.VIEW).build()))
				.build();
		final List<PermissionDomain> newPermissions = List.of(
				PermissionDomain.builder().module("finance").screen("invoices").action(PermissionAction.EDIT).build(),
				PermissionDomain.builder().module("finance").screen("reports").action(PermissionAction.EXPORT).build());
		final ProfileDomain command = ProfileDomain.builder().id(id).permissions(newPermissions).build();
		when(profileRepositoryPort.findById(id)).thenReturn(Optional.of(existing));
		when(profileRepositoryPort.save(existing)).thenReturn(existing);

		final ProfileDomain result = adapter.execute(new Context(command));

		final ArgumentCaptor<ProfileDomain> captor = ArgumentCaptor.forClass(ProfileDomain.class);
		verify(profileRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getId()).isEqualTo(id);
		assertThat(captor.getValue().getPermissions()).isEqualTo(newPermissions);
		assertThat(result.getName()).isEqualTo("Financial");
		verifyNoInteractions(saveCustomProfilePort);
	}

	@DisplayName("Fails when the profile does not exist and no name was given to create one")
	@Test
	void shouldFailWhenProfileNotFoundAndNoNameProvided() {
		final AssignProfileAdapter adapter = new AssignProfileAdapter(profileRepositoryPort, saveCustomProfilePort);
		final UUID id = UUID.randomUUID();
		final ProfileDomain command = ProfileDomain.builder().id(id).permissions(List.of()).build();
		when(profileRepositoryPort.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(command)))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(saveCustomProfilePort, never()).execute(org.mockito.ArgumentMatchers.any());
	}

	@DisplayName("Creates a custom profile when the id is unknown and a name was provided")
	@Test
	void shouldDelegateToSaveCustomProfileWhenIdNotFoundAndNameProvided() {
		final AssignProfileAdapter adapter = new AssignProfileAdapter(profileRepositoryPort, saveCustomProfilePort);
		final UUID id = UUID.randomUUID();
		final List<PermissionDomain> permissions = List.of(
				PermissionDomain.builder().module("sales").screen("orders").action(PermissionAction.VIEW).build());
		final ProfileDomain command = ProfileDomain.builder().id(id).name("Sales Read-Only").permissions(permissions).build();
		when(profileRepositoryPort.findById(id)).thenReturn(Optional.empty());
		final Context context = new Context(command);
		when(saveCustomProfilePort.execute(context)).thenReturn(command);

		final ProfileDomain result = adapter.execute(context);

		assertThat(result).isEqualTo(command);
		verify(saveCustomProfilePort).execute(context);
	}
}
