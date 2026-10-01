package br.gravita.adapters.outbound.persistence.adapters.tax;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.adapters.outbound.persistence.mappers.tax.DiscriminationTemplatePersistenceMapperImpl;
import br.gravita.adapters.outbound.persistence.repositories.tax.DiscriminationTemplateJpaRepository;
import br.gravita.core.domain.tax.DiscriminationTemplate;
import br.gravita.core.domain.tax.DiscriminationTemplateId;
import br.gravita.core.domain.tax.ServiceCode;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({DiscriminationTemplateRepositoryAdapter.class, DiscriminationTemplatePersistenceMapperImpl.class})
class DiscriminationTemplateRepositoryAdapterTest {

	@Autowired
	private DiscriminationTemplateRepositoryAdapter repositoryAdapter;

	@Autowired
	private DiscriminationTemplateJpaRepository jpaRepository;

	private DiscriminationTemplate save(String serviceCode, String text) {
		return repositoryAdapter.save(DiscriminationTemplate.of(DiscriminationTemplateId.of(UUID.randomUUID()),
				ServiceCode.of(serviceCode), text));
	}

	@Test
	@DisplayName("Saves a discrimination template and finds it by id")
	void savesAndFindsById() {
		DiscriminationTemplate saved = save("01.05", "Licenciamento de software");

		DiscriminationTemplate found = repositoryAdapter.findById(saved.getId()).orElseThrow();

		assertThat(found.getServiceCode().value()).isEqualTo("01.05");
		assertThat(found.getTemplateText()).isEqualTo("Licenciamento de software");
		assertThat(repositoryAdapter.findById(DiscriminationTemplateId.of(UUID.randomUUID()))).isEmpty();
	}

	@Test
	@DisplayName("Returns only the templates of the requested service code")
	void ac2_findByServiceCodeReturnsOnlyThatServiceType() {
		save("01.05", "a");
		save("01.05", "b");
		save("08.01", "c");

		assertThat(repositoryAdapter.findByServiceCode(ServiceCode.of("01.05")))
				.extracting(DiscriminationTemplate::getTemplateText).containsExactlyInAnyOrder("a", "b");
		assertThat(repositoryAdapter.findByServiceCode(ServiceCode.of("02.01"))).isEmpty();
		assertThat(repositoryAdapter.findAll()).hasSize(3);
	}

	@Test
	@DisplayName("Keeps a single row when an updated template is saved again")
	void savingAnUpdatedTemplateKeepsASingleRow() {
		DiscriminationTemplate saved = save("01.05", "old");

		DiscriminationTemplate reloaded = repositoryAdapter.findById(saved.getId()).orElseThrow();
		reloaded.update(ServiceCode.of("08.01"), "new");
		repositoryAdapter.save(reloaded);
		jpaRepository.flush();

		assertThat(jpaRepository.count()).isEqualTo(1);
		DiscriminationTemplate updated = repositoryAdapter.findById(saved.getId()).orElseThrow();
		assertThat(updated.getServiceCode().value()).isEqualTo("08.01");
		assertThat(updated.getTemplateText()).isEqualTo("new");
	}

	@Test
	@DisplayName("Removes only the template that was deleted by id")
	void deleteByIdRemovesTheTemplate() {
		DiscriminationTemplate keep = save("01.05", "keep");
		DiscriminationTemplate drop = save("01.05", "drop");

		repositoryAdapter.deleteById(drop.getId());
		jpaRepository.flush();

		assertThat(repositoryAdapter.findById(drop.getId())).isEmpty();
		assertThat(repositoryAdapter.findById(keep.getId())).isPresent();
	}
}
