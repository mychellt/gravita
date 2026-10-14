package br.gravita.core.ports.outbound.tax;

import br.gravita.core.domain.tax.Cfop;
import br.gravita.core.domain.tax.CfopRegistryEntry;
import br.gravita.core.domain.tax.NaturezaOperacao;
import java.util.List;

/**
 * The "free CFOP registry with automatic mapping by operation type" from doc §3.1 (UC-M2-01, AC2). The issuance use
 * case must resolve a CFOP through this port rather than branching on {@link NaturezaOperacao} itself. The registry is
 * stored in the database and is read-only for now - a "manage CFOP registry" admin screen is a separate, deferred ticket.
 */
public interface CfopRegistryPort {

	Cfop resolve(NaturezaOperacao naturezaOperacao);

	/** Every active registry entry, in {@link NaturezaOperacao} declaration order. */
	List<CfopRegistryEntry> findAll();
}
