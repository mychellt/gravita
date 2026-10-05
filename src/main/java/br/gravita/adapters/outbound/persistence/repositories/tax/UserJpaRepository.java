package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {
	Optional<UserJpaEntity> findByEmail(String email);

	boolean existsByEmail(String email);

	List<UserJpaEntity> findAllByCompanyId(UUID companyId);
}
