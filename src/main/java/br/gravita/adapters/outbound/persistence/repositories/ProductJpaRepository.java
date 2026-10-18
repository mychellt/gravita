package br.gravita.adapters.outbound.persistence.repositories;

import br.gravita.adapters.outbound.persistence.entities.ProductJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {
	boolean existsByBarcodesContaining(final String barcode);

	Optional<ProductJpaEntity> findByBarcodesContaining(final String barcode);

	@Query("select p from ProductJpaEntity p where replace(replace(p.ncm, '.', ''), ' ', '') = :ncmDigits")
	List<ProductJpaEntity> findAllByNcmDigits(@Param("ncmDigits") final String ncmDigits);
}
