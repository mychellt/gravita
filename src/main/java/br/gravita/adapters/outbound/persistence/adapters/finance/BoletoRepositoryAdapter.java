package br.gravita.adapters.outbound.persistence.adapters.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.BoletoJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.finance.BoletoPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.finance.BoletoJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.finance.Boleto;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.ports.outbound.persistence.finance.BoletoRepositoryPort;
import java.util.List;

@PersistenceAdapter
class BoletoRepositoryAdapter implements BoletoRepositoryPort {

	private final BoletoJpaRepository jpaRepository;
	private final BoletoPersistenceMapper mapper;

	BoletoRepositoryAdapter(final BoletoJpaRepository jpaRepository, final BoletoPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Boleto save(final Boleto boleto) {
		final BoletoJpaEntity entity = mapper.map(boleto);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		return mapper.map(jpaRepository.save(entity));
	}

	@Override
	public List<Boleto> findByReceivableId(final ReceivableId receivableId) {
		return jpaRepository.findByReceivableIdOrderByCreatedAt(receivableId.value()).stream()
				.map(mapper::map).toList();
	}
}
