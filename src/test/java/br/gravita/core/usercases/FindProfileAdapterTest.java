package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PermissionAction;
import br.gravita.core.domain.PermissionDomain;
import br.gravita.core.domain.ProfileDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.persistence.ProfileRepositoryPort;
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

	@Test
	void shouldReturnProfileWhenFound() {
		FindProfileAdapter adapter = new FindProfileAdapter(profileRepositoryPort);
		UUID id = UUID.randomUUID();
		ProfileDomain profile = ProfileDomain.builder().id(id).name("Financial")
				.permissions(List.of(PermissionDomain.builder().module("finance").screen("invoices")
						.action(PermissionAction.VIEW).build()))
				.build();
		when(profileRepositoryPort.findById(id)).thenReturn(Optional.of(profile));

		ProfileDomain found = adapter.execute(new Context(id));

		assertThat(found).isEqualTo(profile);
	}

	@Test
	void shouldFailWhenProfileNotFound() {
		FindProfileAdapter adapter = new FindProfileAdapter(profileRepositoryPort);
		UUID id = UUID.randomUUID();
		when(profileRepositoryPort.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(id))).isInstanceOf(ResourceNotFoundException.class);
	}
}
