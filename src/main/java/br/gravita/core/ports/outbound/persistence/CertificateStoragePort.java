package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DigitalCertificate;

import java.util.Optional;

/**
 * Encrypted at-rest storage for a company's A1 certificate (doc §11.4). {@link #save} replaces
 * any certificate the company already holds - a company never holds two active certificates
 * (UC-M1-02).
 */
public interface CertificateStoragePort {
	DigitalCertificate save(DigitalCertificate certificate);

	Optional<DigitalCertificate> findByCompanyId(CompanyId companyId);
}
