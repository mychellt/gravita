package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.entities.CompanyPersonJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.CompanyPersonJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.CompanyPerson;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.ports.outbound.persistence.CompanyPersonRepositoryPort;

@PersistenceAdapter
class CompanyPersonRepositoryAdapter implements CompanyPersonRepositoryPort {

	private final CompanyPersonJpaRepository jpaRepository;

	CompanyPersonRepositoryAdapter(CompanyPersonJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public CompanyPerson save(CompanyPerson company) {
		CompanyPersonJpaEntity entity = new CompanyPersonJpaEntity();
		entity.setName(company.getName());
		entity.setDocument(company.getDocument().number());
		entity.setPhone(company.getPhone());
		CompanyPersonJpaEntity saved = jpaRepository.save(entity);
		return CompanyPerson.builder()
				.id(saved.getId())
				.name(saved.getName())
				.document(new Document(saved.getDocument(), PersonType.COMPANY))
				.phone(saved.getPhone())
				.active(saved.isActive())
				.build();
	}

	@Override
	public boolean existsByDocument(String documentNumber) {
		return jpaRepository.existsByDocument(documentNumber);
	}
}
