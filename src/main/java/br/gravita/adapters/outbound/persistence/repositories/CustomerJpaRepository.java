package br.gravita.adapters.outbound.persistence.repositories;


import br.gravita.adapters.outbound.persistence.entities.CustomerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerJpaRepository extends JpaRepository<CustomerJpaEntity, UUID>, JpaSpecificationExecutor<CustomerJpaEntity> {

	List<CustomerJpaEntity> findAllByCompanyId(UUID companyId);

	Optional<CustomerJpaEntity> findByIdAndCompanyId(UUID id, UUID companyId);
}
