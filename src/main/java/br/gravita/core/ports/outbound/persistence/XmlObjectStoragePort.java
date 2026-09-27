package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.masterdata.CompanyId;

public interface XmlObjectStoragePort {
	String store(CompanyId companyId, byte[] xmlContent);
}
