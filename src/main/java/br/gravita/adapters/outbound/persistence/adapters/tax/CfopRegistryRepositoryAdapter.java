package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.CfopRegistryJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.tax.CfopRegistryJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.tax.Cfop;
import br.gravita.core.domain.tax.CfopRegistryEntry;
import br.gravita.core.domain.tax.NaturezaOperacao;
import br.gravita.core.ports.outbound.tax.CfopRegistryPort;
import java.util.Comparator;
import java.util.List;

@PersistenceAdapter
class CfopRegistryRepositoryAdapter implements CfopRegistryPort {

	private final CfopRegistryJpaRepository jpaRepository;

	CfopRegistryRepositoryAdapter(final CfopRegistryJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Cfop resolve(final NaturezaOperacao naturezaOperacao) {
		return jpaRepository.findByNaturezaOperacaoAndActiveTrue(naturezaOperacao)
				.map(entity -> new Cfop(entity.getCfop()))
				.orElseThrow(() -> new BusinessRuleException("No CFOP registered for operation type: " + naturezaOperacao));
	}

	@Override
	public List<CfopRegistryEntry> findAll() {
		return jpaRepository.findAllByActiveTrue().stream()
				.sorted(Comparator.comparing(CfopRegistryJpaEntity::getNaturezaOperacao))
				.map(entity -> new CfopRegistryEntry(entity.getNaturezaOperacao(), new Cfop(entity.getCfop()),
						entity.getDescription()))
				.toList();
	}
}
