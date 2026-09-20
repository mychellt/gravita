package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.entities.ProductJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.ProductPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.ProductJpaRepository;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class ProductRepositoryAdapter implements ProductRepositoryPort {

	private final ProductJpaRepository jpaRepository;
	private final ProductPersistenceMapper mapper = new ProductPersistenceMapper();

	ProductRepositoryAdapter(ProductJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public ProductDomain save(ProductDomain product) {
		ProductJpaEntity saved = jpaRepository.save(mapper.toEntity(product));
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<ProductDomain> get(UUID id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public List<ProductDomain> findAll() {
		return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
	}

	@Override
	public boolean existsByBarcode(String barcode) {
		return jpaRepository.existsByBarcodesContaining(barcode);
	}
}
