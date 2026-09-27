package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DigitalCertificate;

import java.util.Optional;

public interface CertificateStoragePort {
	DigitalCertificate save(DigitalCertificate certificate);

	Optional<DigitalCertificate> findByCompanyId(CompanyId companyId);
}
