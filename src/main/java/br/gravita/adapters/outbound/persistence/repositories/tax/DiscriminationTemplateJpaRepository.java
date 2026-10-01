package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.DiscriminationTemplateJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiscriminationTemplateJpaRepository extends JpaRepository<DiscriminationTemplateJpaEntity, UUID> {

	List<DiscriminationTemplateJpaEntity> findAllByOrderByServiceCodeAscCreatedAtAsc();

	List<DiscriminationTemplateJpaEntity> findByServiceCodeOrderByCreatedAtAsc(String serviceCode);

	/**
	 * A direct delete: {@code deleteById} skips an entity whose {@code isNew} flag is still set (persisted earlier in
	 * the same persistence context and not yet flushed).
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from DiscriminationTemplateJpaEntity t where t.id = :id")
	void deleteTemplateById(@Param("id") UUID id);
}
