package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.XmlObjectJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.tax.XmlObjectJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.ports.outbound.persistence.XmlObjectStoragePort;
import java.util.UUID;

@PersistenceAdapter
class XmlObjectStorageAdapter implements XmlObjectStoragePort {

	private final XmlObjectJpaRepository jpaRepository;

	XmlObjectStorageAdapter(XmlObjectJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public String store(CompanyId companyId, byte[] xmlContent) {
		XmlObjectJpaEntity entity = XmlObjectJpaEntity.builder()
				.id(UUID.randomUUID())
				.companyId(companyId.value())
				.content(xmlContent)
				.build();
		entity.setNew(true);
		XmlObjectJpaEntity saved = jpaRepository.save(entity);
		return saved.getId().toString();
	}
}
