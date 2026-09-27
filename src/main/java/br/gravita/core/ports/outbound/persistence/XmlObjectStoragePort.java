package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.masterdata.CompanyId;

public interface XmlObjectStoragePort {
	String store(CompanyId companyId, byte[] xmlContent);

	/**
	 * UC-M2-03 (AC6): retrieves previously stored content (XML or DANFE bytes
	 * alike - the port is content-agnostic) by the reference {@link #store}
	 * returned, for download or resend.
	 */
	byte[] retrieve(String reference);
}
