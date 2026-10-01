package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.entities.ProfileJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.ProfilePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.ProfileJpaRepository;
import br.gravita.core.domain.ProfileDomain;
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
class ProfileRepositoryAdapterTest {

	@Mock
	private ProfileJpaRepository repository;

	@Mock
	private ProfilePersistenceMapper mapper;

	@InjectMocks
	private ProfileRepositoryAdapter adapter;

	@Test
	@DisplayName("Saves a new profile marking its entity as new")
	void shouldSaveNewProfile() {
		final ProfileDomain profile = buildProfile("Sales Read-Only");
		final ProfileJpaEntity entity = buildEntity(profile.getId(), profile.getName());
		final ProfileJpaEntity saved = buildEntity(profile.getId(), profile.getName());
		when(mapper.map(profile)).thenReturn(entity);
		when(repository.existsById(profile.getId())).thenReturn(false);
		when(repository.save(entity)).thenReturn(saved);
		when(mapper.map(saved)).thenReturn(profile);

		final ProfileDomain result = adapter.save(profile);

		assertThat(result).isSameAs(profile);
		assertThat(entity.isNew()).isTrue();
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Saves an existing profile marking its entity as not new")
	void shouldSaveExistingProfileAsNotNew() {
		final ProfileDomain profile = buildProfile("Financial");
		final ProfileJpaEntity entity = buildEntity(profile.getId(), profile.getName());
		when(mapper.map(profile)).thenReturn(entity);
		when(repository.existsById(profile.getId())).thenReturn(true);
		when(repository.save(entity)).thenReturn(entity);
		when(mapper.map(entity)).thenReturn(profile);

		adapter.save(profile);

		assertThat(entity.isNew()).isFalse();
	}

	@Test
	@DisplayName("Finds a profile by id")
	void shouldFindProfileById() {
		final ProfileDomain profile = buildProfile("Sales Read-Only");
		final ProfileJpaEntity entity = buildEntity(profile.getId(), profile.getName());
		when(repository.findById(profile.getId())).thenReturn(Optional.of(entity));
		when(mapper.map(entity)).thenReturn(profile);

		final Optional<ProfileDomain> result = adapter.findById(profile.getId());

		assertThat(result).contains(profile);
		verify(repository).findById(profile.getId());
	}

	@Test
	@DisplayName("Finds a profile by its name")
	void shouldFindProfileByName() {
		final ProfileDomain profile = buildProfile("Purchasing");
		final ProfileJpaEntity entity = buildEntity(profile.getId(), profile.getName());
		when(repository.findByName("Purchasing")).thenReturn(Optional.of(entity));
		when(mapper.map(entity)).thenReturn(profile);

		final Optional<ProfileDomain> result = adapter.findByName("Purchasing");

		assertThat(result).contains(profile);
		verify(repository).findByName("Purchasing");
	}

	@Test
	@DisplayName("Returns empty when no profile has the given name")
	void shouldReturnEmptyWhenNameNotFound() {
		when(repository.findByName("Unknown Profile")).thenReturn(Optional.empty());

		assertThat(adapter.findByName("Unknown Profile")).isEmpty();
	}

	private ProfileJpaEntity buildEntity(final UUID id, final String name) {
		return ProfileJpaEntity.builder().id(id).name(name).build();
	}

	private ProfileDomain buildProfile(final String name) {
		return ProfileDomain.builder().id(UUID.randomUUID()).name(name).permissions(List.of()).build();
	}
}
