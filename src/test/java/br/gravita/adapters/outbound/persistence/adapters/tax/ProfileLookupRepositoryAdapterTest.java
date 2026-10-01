package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.ProfileJpaEntity;
import br.gravita.core.domain.system.ProfileReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(ProfileLookupRepositoryAdapter.class)
class ProfileLookupRepositoryAdapterTest {

	@Autowired
	private ProfileLookupRepositoryAdapter repositoryAdapter;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("Finds an existing profile by id")
	void shouldFindExistingProfileById() {
		UUID id = UUID.randomUUID();
		entityManager.persist(ProfileJpaEntity.builder().id(id).name("Financial").permissions(List.of()).build());

		Optional<ProfileReference> found = repositoryAdapter.findById(id);

		assertThat(found).contains(new ProfileReference(id, "Financial"));
	}

	@Test
	@DisplayName("Returns empty when the profile does not exist")
	void shouldReturnEmptyWhenProfileDoesNotExist() {
		assertThat(repositoryAdapter.findById(UUID.randomUUID())).isEmpty();
	}
}
