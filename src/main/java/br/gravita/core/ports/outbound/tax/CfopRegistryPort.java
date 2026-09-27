package br.gravita.core.ports.outbound.tax;

import br.gravita.core.domain.tax.Cfop;
import br.gravita.core.domain.tax.NaturezaOperacao;

/**
 * The "free CFOP registry with automatic mapping by operation type" from doc
 * §3.1 (UC-M2-01, AC2). Not in the ticket's originally-listed port set, but
 * required to make the AC testable/enforceable: the issuance use case must
 * resolve a CFOP through this port rather than branching on
 * {@link NaturezaOperacao} itself. The current adapter is a small static
 * default table (no admin CRUD yet) - a real "manage CFOP registry" screen is
 * left for a future M1-style auxiliary-table ticket.
 */
public interface CfopRegistryPort {

	Cfop resolve(NaturezaOperacao naturezaOperacao);
}
