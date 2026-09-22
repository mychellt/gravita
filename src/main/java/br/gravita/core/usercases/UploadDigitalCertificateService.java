package br.gravita.core.usercases;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.masterdata.CertificateType;
import br.gravita.core.domain.masterdata.CompanyNotFoundException;
import br.gravita.core.domain.masterdata.DigitalCertificate;
import br.gravita.core.ports.inbound.masterdata.UploadDigitalCertificateCommand;
import br.gravita.core.ports.inbound.masterdata.UploadDigitalCertificateUseCase;
import br.gravita.core.ports.outbound.persistence.CertificateStoragePort;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.security.CertificateReaderPort;

import java.time.Instant;

/**
 * Uploads or replaces a company's A1 certificate (UC-M1-02). Encryption of the {@code .pfx}
 * payload happens in {@code CertificateStoragePort}'s adapter, not here - this service only
 * validates the certificate type, extracts its expiry date, and persists it.
 */
@UseCase
public class UploadDigitalCertificateService implements UploadDigitalCertificateUseCase {

	private final CompanyRepositoryPort companyRepositoryPort;
	private final CertificateStoragePort certificateStoragePort;
	private final CertificateReaderPort certificateReaderPort;

	public UploadDigitalCertificateService(CompanyRepositoryPort companyRepositoryPort,
			CertificateStoragePort certificateStoragePort, CertificateReaderPort certificateReaderPort) {
		this.companyRepositoryPort = companyRepositoryPort;
		this.certificateStoragePort = certificateStoragePort;
		this.certificateReaderPort = certificateReaderPort;
	}

	@Override
	public void execute(UploadDigitalCertificateCommand command) {
		companyRepositoryPort.findById(command.companyId())
				.orElseThrow(() -> new CompanyNotFoundException(command.companyId().value()));

		CertificateType type = CertificateType.fromCode(command.certificateType());
		Instant expiresAt = certificateReaderPort.readExpiryDate(command.pfxFile(), command.password());

		DigitalCertificate certificate = DigitalCertificate.upload(command.companyId(), type, command.pfxFile(),
				command.password(), expiresAt);

		certificateStoragePort.save(certificate);
	}
}
