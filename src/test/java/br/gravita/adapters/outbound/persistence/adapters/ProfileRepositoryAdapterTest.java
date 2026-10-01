package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.mappers.ProfilePersistenceMapperImpl;
import br.gravita.core.domain.PermissionAction;
import br.gravita.core.domain.PermissionDomain;
import br.gravita.core.domain.ProfileDomain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({ProfileRepositoryAdapter.class, ProfilePersistenceMapperImpl.class})
class ProfileRepositoryAdapterTest {

	@Autowired
	private ProfileRepositoryAdapter repositoryAdapter;

	@Test
	@DisplayName("Saves a profile with its permissions and retrieves it")
	void shouldSaveAndRetrieveProfileWithPermissions() {
		UUID id = UUID.randomUUID();
		ProfileDomain profile = ProfileDomain.builder().id(id).name("Sales Read-Only")
				.permissions(List.of(PermissionDomain.builder().module("sales").screen("orders")
						.action(PermissionAction.VIEW).build()))
				.build();

		ProfileDomain saved = repositoryAdapter.save(profile);

		assertThat(repositoryAdapter.findById(saved.getId()))
				.isPresent()
				.get()
				.satisfies(found -> {
					assertThat(found.getName()).isEqualTo("Sales Read-Only");
					assertThat(found.getPermissions()).hasSize(1);
					assertThat(found.getPermissions().get(0).getAction()).isEqualTo(PermissionAction.VIEW);
				});
	}

	@Test
	@DisplayName("Finds a profile by its name")
	void shouldFindProfileByName() {
		UUID id = UUID.randomUUID();
		ProfileDomain profile = ProfileDomain.builder().id(id).name("Purchasing").permissions(List.of()).build();
		repositoryAdapter.save(profile);

		assertThat(repositoryAdapter.findByName("Purchasing")).isPresent().get()
				.satisfies(found -> assertThat(found.getId()).isEqualTo(id));
	}

	@Test
	@DisplayName("Returns empty when no profile has the given name")
	void shouldReturnEmptyWhenNameNotFound() {
		assertThat(repositoryAdapter.findByName("Unknown Profile")).isEmpty();
	}

	@Test
	@DisplayName("Replaces a profile's permissions without affecting other profiles")
	void shouldReplacePermissionsWithoutAffectingOtherProfiles() {
		ProfileDomain financial = ProfileDomain.builder().id(UUID.randomUUID()).name("Financial")
				.permissions(List.of(PermissionDomain.builder().module("finance").screen("invoices")
						.action(PermissionAction.VIEW).build()))
				.build();
		ProfileDomain salesperson = ProfileDomain.builder().id(UUID.randomUUID()).name("Salesperson")
				.permissions(List.of(PermissionDomain.builder().module("sales").screen("orders")
						.action(PermissionAction.VIEW).build()))
				.build();
		repositoryAdapter.save(financial);
		repositoryAdapter.save(salesperson);

		financial.setPermissions(List.of(PermissionDomain.builder().module("finance").screen("invoices")
				.action(PermissionAction.EDIT).build()));
		repositoryAdapter.save(financial);

		assertThat(repositoryAdapter.findById(financial.getId())).isPresent().get()
				.satisfies(found -> assertThat(found.getPermissions().get(0).getAction())
						.isEqualTo(PermissionAction.EDIT));
		assertThat(repositoryAdapter.findById(salesperson.getId())).isPresent().get()
				.satisfies(found -> assertThat(found.getPermissions().get(0).getAction())
						.isEqualTo(PermissionAction.VIEW));
	}
}
