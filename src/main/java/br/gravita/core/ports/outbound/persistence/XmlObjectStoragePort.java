package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.masterdata.CompanyId;

/**
 * Object storage for fiscal XML/DANFE documents (doc §13) - the owning
 * aggregate's DB row only holds the reference {@link #store} returns, never
 * the raw content.
 */
public interface XmlObjectStoragePort {
	String store(CompanyId companyId, byte[] xmlContent);
}
