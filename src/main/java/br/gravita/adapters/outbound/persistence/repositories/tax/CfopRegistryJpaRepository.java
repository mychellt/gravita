package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.CfopRegistryJpaEntity;
import br.gravita.core.domain.tax.NaturezaOperacao;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CfopRegistryJpaRepository extends JpaRepository<CfopRegistryJpaEntity, UUID> {

	Optional<CfopRegistryJpaEntity> findByNaturezaOperacaoAndActiveTrue(NaturezaOperacao naturezaOperacao);

	List<CfopRegistryJpaEntity> findAllByActiveTrue();
}
