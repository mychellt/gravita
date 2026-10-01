package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.DiscriminationTemplateJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.DiscriminationTemplatePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.DiscriminationTemplateJpaRepository;
import br.gravita.core.domain.tax.DiscriminationTemplate;
import br.gravita.core.domain.tax.DiscriminationTemplateId;
import br.gravita.core.domain.tax.ServiceCode;
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
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscriminationTemplateRepositoryAdapterTest {

	@Mock
	private DiscriminationTemplateJpaRepository repository;

	@Mock
	private DiscriminationTemplatePersistenceMapper mapper;

	@InjectMocks
	private DiscriminationTemplateRepositoryAdapter adapter;

	@Test
	@DisplayName("Saves a new template using the mapped entity")
	void savesANewTemplateUsingTheMappedEntity() {
		final DiscriminationTemplate template = buildTemplate("01.05", "Licenciamento de software");
		final DiscriminationTemplateJpaEntity entity = buildEntity(template.getId().value());
		final DiscriminationTemplateJpaEntity saved = buildEntity(template.getId().value());
		when(repository.findById(template.getId().value())).thenReturn(Optional.empty());
		when(mapper.map(template)).thenReturn(entity);
		when(repository.save(same(entity))).thenReturn(saved);
		when(mapper.map(saved)).thenReturn(template);

		final DiscriminationTemplate result = adapter.save(template);

		assertThat(result).isSameAs(template);
		verify(repository).save(same(entity));
	}

	@Test
	@DisplayName("Updates the managed row in place when the template already exists")
	void savingAnUpdatedTemplateUpdatesTheManagedRow() {
		final DiscriminationTemplate template = buildTemplate("08.01", "new");
		final DiscriminationTemplateJpaEntity existing = buildEntity(template.getId().value());
		existing.setServiceCode("01.05");
		existing.setTemplateText("old");
		when(repository.findById(template.getId().value())).thenReturn(Optional.of(existing));
		when(repository.save(same(existing))).thenReturn(existing);
		when(mapper.map(existing)).thenReturn(template);

		adapter.save(template);

		assertThat(existing.getServiceCode()).isEqualTo("08.01");
		assertThat(existing.getTemplateText()).isEqualTo("new");
		verify(repository).save(same(existing));
	}

	@Test
	@DisplayName("Finds a template by id")
	void findsById() {
		final DiscriminationTemplate template = buildTemplate("01.05", "a");
		final DiscriminationTemplateJpaEntity entity = buildEntity(template.getId().value());
		when(repository.findById(template.getId().value())).thenReturn(Optional.of(entity));
		when(mapper.map(entity)).thenReturn(template);

		final Optional<DiscriminationTemplate> result = adapter.findById(template.getId());

		assertThat(result).contains(template);
		verify(repository).findById(template.getId().value());
	}

	@Test
	@DisplayName("Returns empty when no template exists for the id")
	void returnsEmptyWhenNoTemplateExistsForTheId() {
		final DiscriminationTemplateId id = DiscriminationTemplateId.of(UUID.randomUUID());
		when(repository.findById(id.value())).thenReturn(Optional.empty());

		assertThat(adapter.findById(id)).isEmpty();
	}

	@Test
	@DisplayName("Lists all templates ordered by service code and creation")
	void listsAllTemplates() {
		final DiscriminationTemplate first = buildTemplate("01.05", "a");
		final DiscriminationTemplate second = buildTemplate("08.01", "c");
		final DiscriminationTemplateJpaEntity firstEntity = buildEntity(first.getId().value());
		final DiscriminationTemplateJpaEntity secondEntity = buildEntity(second.getId().value());
		when(repository.findAllByOrderByServiceCodeAscCreatedAtAsc()).thenReturn(List.of(firstEntity, secondEntity));
		when(mapper.map(same(firstEntity))).thenReturn(first);
		when(mapper.map(same(secondEntity))).thenReturn(second);

		final List<DiscriminationTemplate> result = adapter.findAll();

		assertThat(result).containsExactly(first, second);
	}

	@Test
	@DisplayName("Finds the templates of the requested service code")
	void findsTheTemplatesOfTheRequestedServiceCode() {
		final DiscriminationTemplate template = buildTemplate("01.05", "a");
		final DiscriminationTemplateJpaEntity entity = buildEntity(template.getId().value());
		when(repository.findByServiceCodeOrderByCreatedAtAsc("01.05")).thenReturn(List.of(entity));
		when(mapper.map(entity)).thenReturn(template);

		final List<DiscriminationTemplate> result = adapter.findByServiceCode(ServiceCode.of("01.05"));

		assertThat(result).containsExactly(template);
		verify(repository).findByServiceCodeOrderByCreatedAtAsc("01.05");
	}

	@Test
	@DisplayName("Deletes the template by id")
	void deletesTheTemplateById() {
		final DiscriminationTemplateId id = DiscriminationTemplateId.of(UUID.randomUUID());

		adapter.deleteById(id);

		verify(repository).deleteTemplateById(id.value());
	}

	private DiscriminationTemplateJpaEntity buildEntity(final UUID id) {
		return DiscriminationTemplateJpaEntity.builder().id(id).build();
	}

	private DiscriminationTemplate buildTemplate(final String serviceCode, final String text) {
		return DiscriminationTemplate.of(DiscriminationTemplateId.of(UUID.randomUUID()), ServiceCode.of(serviceCode),
				text);
	}
}
