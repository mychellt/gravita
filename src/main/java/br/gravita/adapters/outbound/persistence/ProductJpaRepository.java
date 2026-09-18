package br.gravita.adapters.outbound.persistence;

import br.gravita.adapters.outbound.persistence.entities.ProductJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {
	boolean existsByBarcodesContaining(final String barcode);
}
