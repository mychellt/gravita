package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.ProfileJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.tax.ProfileLookupJpaRepository;
import br.gravita.core.domain.system.ProfileReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileLookupRepositoryAdapterTest {

	@Mock
	private ProfileLookupJpaRepository repository;

	@InjectMocks
	private ProfileLookupRepositoryAdapter adapter;

	@Test
	@DisplayName("Finds an existing profile by id")
	void shouldFindExistingProfileById() {
		final UUID id = UUID.randomUUID();
		final ProfileJpaEntity entity = ProfileJpaEntity.builder().id(id).name("Financial").permissions(List.of()).build();
		when(repository.findById(id)).thenReturn(Optional.of(entity));

		final Optional<ProfileReference> result = adapter.findById(id);

		assertThat(result).contains(new ProfileReference(id, "Financial"));
		verify(repository).findById(id);
	}

	@Test
	@DisplayName("Returns empty when the profile does not exist")
	void shouldReturnEmptyWhenProfileDoesNotExist() {
		final UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThat(adapter.findById(id)).isEmpty();
	}
}
