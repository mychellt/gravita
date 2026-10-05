package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.mappers.tax.InboundManifestationPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.InboundManifestationJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.tax.InboundManifestation;
import br.gravita.core.ports.outbound.persistence.tax.InboundManifestationRepositoryPort;
import java.util.List;

@PersistenceAdapter
class InboundManifestationRepositoryAdapter implements InboundManifestationRepositoryPort {

	private final InboundManifestationJpaRepository jpaRepository;
	private final InboundManifestationPersistenceMapper mapper;

	InboundManifestationRepositoryAdapter(final InboundManifestationJpaRepository jpaRepository,
			final InboundManifestationPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public InboundManifestation save(final InboundManifestation manifestation) {
		return mapper.map(jpaRepository.save(mapper.map(manifestation)));
	}

	@Override
	public List<InboundManifestation> findByAccessKey(final String accessKey) {
		return jpaRepository.findByAccessKey(accessKey).stream().map(mapper::map).toList();
	}
}
