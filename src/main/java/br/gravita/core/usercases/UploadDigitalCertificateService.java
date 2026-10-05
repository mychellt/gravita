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

@UseCase
public class UploadDigitalCertificateService implements UploadDigitalCertificateUseCase {

	private final CompanyRepositoryPort companyRepositoryPort;
	private final CertificateStoragePort certificateStoragePort;
	private final CertificateReaderPort certificateReaderPort;

	public UploadDigitalCertificateService(final CompanyRepositoryPort companyRepositoryPort,
			final CertificateStoragePort certificateStoragePort, final CertificateReaderPort certificateReaderPort) {
		this.companyRepositoryPort = companyRepositoryPort;
		this.certificateStoragePort = certificateStoragePort;
		this.certificateReaderPort = certificateReaderPort;
	}

	@Override
	public void execute(final UploadDigitalCertificateCommand command) {
		companyRepositoryPort.findById(command.companyId())
				.orElseThrow(() -> new CompanyNotFoundException(command.companyId().value()));

		final CertificateType type = CertificateType.fromCode(command.certificateType());
		final Instant expiresAt = certificateReaderPort.readExpiryDate(command.pfxFile(), command.password());

		final DigitalCertificate certificate = DigitalCertificate.upload(command.companyId(), type, command.pfxFile(),
				command.password(), expiresAt);

		certificateStoragePort.save(certificate);
	}
}
