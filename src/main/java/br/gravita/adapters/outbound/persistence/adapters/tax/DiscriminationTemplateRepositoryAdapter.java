package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.DiscriminationTemplateJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.DiscriminationTemplatePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.DiscriminationTemplateJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.tax.DiscriminationTemplate;
import br.gravita.core.domain.tax.DiscriminationTemplateId;
import br.gravita.core.domain.tax.ServiceCode;
import br.gravita.core.ports.outbound.persistence.tax.DiscriminationTemplateRepositoryPort;
import java.util.List;
import java.util.Optional;

@PersistenceAdapter
class DiscriminationTemplateRepositoryAdapter implements DiscriminationTemplateRepositoryPort {

	private final DiscriminationTemplateJpaRepository jpaRepository;
	private final DiscriminationTemplatePersistenceMapper mapper;

	DiscriminationTemplateRepositoryAdapter(DiscriminationTemplateJpaRepository jpaRepository,
			DiscriminationTemplatePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public DiscriminationTemplate save(DiscriminationTemplate template) {
		DiscriminationTemplateJpaEntity entity = jpaRepository.findById(template.getId().value())
				.map(existing -> {
					// Update the managed row so audit columns (created_at) are preserved.
					existing.setServiceCode(template.getServiceCode().value());
					existing.setTemplateText(template.getTemplateText());
					return existing;
				})
				.orElseGet(() -> mapper.map(template));
		return mapper.map(jpaRepository.save(entity));
	}

	@Override
	public Optional<DiscriminationTemplate> findById(DiscriminationTemplateId id) {
		return jpaRepository.findById(id.value()).map(mapper::map);
	}

	@Override
	public List<DiscriminationTemplate> findAll() {
		return jpaRepository.findAllByOrderByServiceCodeAscCreatedAtAsc().stream().map(mapper::map).toList();
	}

	@Override
	public List<DiscriminationTemplate> findByServiceCode(ServiceCode serviceCode) {
		return jpaRepository.findByServiceCodeOrderByCreatedAtAsc(serviceCode.value()).stream()
				.map(mapper::map).toList();
	}

	@Override
	public void deleteById(DiscriminationTemplateId id) {
		jpaRepository.deleteTemplateById(id.value());
	}
}
