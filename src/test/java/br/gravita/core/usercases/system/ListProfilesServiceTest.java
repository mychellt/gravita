package br.gravita.core.usercases.system;

import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.ports.outbound.persistence.system.ProfileRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListProfilesServiceTest {

	@Mock
	private ProfileRepositoryPort profileRepositoryPort;

	@Test
	@DisplayName("Lists the available profiles")
	void shouldListTheAvailableProfiles() {
		final List<ProfileReference> profiles = List.of(new ProfileReference(UUID.randomUUID(), "Administrator"),
				new ProfileReference(UUID.randomUUID(), "Salesperson"));
		when(profileRepositoryPort.findAll()).thenReturn(profiles);

		assertThat(new ListProfilesService(profileRepositoryPort).execute()).isEqualTo(profiles);
	}
}
