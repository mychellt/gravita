package br.gravita.masterdata.application.service;

import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.CompanyNotFoundException;
import br.gravita.core.domain.masterdata.DigitalCertificate;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.ports.inbound.masterdata.UploadDigitalCertificateCommand;
import br.gravita.core.ports.outbound.persistence.CertificateStoragePort;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.security.CertificateReaderPort;
import br.gravita.core.usercases.UploadDigitalCertificateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadDigitalCertificateServiceTest {

	private static final byte[] PFX_FILE = {1, 2, 3};

	@Mock
	private CompanyRepositoryPort companyRepositoryPort;

	@Mock
	private CertificateStoragePort certificateStoragePort;

	@Mock
	private CertificateReaderPort certificateReaderPort;

	private UploadDigitalCertificateService service() {
		return new UploadDigitalCertificateService(companyRepositoryPort, certificateStoragePort, certificateReaderPort);
	}

	private Company existingCompany(final CompanyId id) {
		return Company.builder()
				.id(id)
				.name("Acme Ltda")
				.cnpj(Document.cnpj("11222333000181"))
				.ie("123456789")
				.im("987654")
				.cnae("6201-5/01")
				.taxRegime(TaxRegime.SIMPLES_NACIONAL)
				.simplesOptante(true)
				.sefazEnvironment(SefazEnvironment.HOMOLOGATION)
				.address("Rua Teste, 100")
				.state("SP")
				.issuingEmail("fiscal@empresa.com")
				.phone("11999999999")
				.logoUrl(null)
				.parentCompanyId(null)
				.build();
	}

	@Test
	@DisplayName("Uploads an A1 certificate and persists the extracted expiry date")
	void shouldUploadA1CertificateAndPersistExtractedExpiry() {
		final CompanyId id = CompanyId.of(UUID.randomUUID());
		final Instant expiresAt = Instant.now().plus(365, ChronoUnit.DAYS);
		when(companyRepositoryPort.findById(id)).thenReturn(Optional.of(existingCompany(id)));
		when(certificateReaderPort.readExpiryDate(PFX_FILE, "secret")).thenReturn(expiresAt);
		when(certificateStoragePort.save(any(DigitalCertificate.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service().execute(new UploadDigitalCertificateCommand(id, "A1", PFX_FILE, "secret"));

		final ArgumentCaptor<DigitalCertificate> captor = ArgumentCaptor.forClass(DigitalCertificate.class);
		verify(certificateStoragePort).save(captor.capture());
		final DigitalCertificate saved = captor.getValue();
		assertThat(saved.getCompanyId()).isEqualTo(id);
		assertThat(saved.getExpiresAt()).isEqualTo(expiresAt);
		assertThat(saved.getPfxPayload()).isEqualTo(PFX_FILE);
	}

	@Test
	@DisplayName("Defaults to A1 when the certificate type is not provided")
	void shouldDefaultToA1WhenTypeIsNotProvided() {
		final CompanyId id = CompanyId.of(UUID.randomUUID());
		when(companyRepositoryPort.findById(id)).thenReturn(Optional.of(existingCompany(id)));
		when(certificateReaderPort.readExpiryDate(eq(PFX_FILE), eq("secret")))
				.thenReturn(Instant.now().plus(1, ChronoUnit.DAYS));
		when(certificateStoragePort.save(any(DigitalCertificate.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service().execute(new UploadDigitalCertificateCommand(id, null, PFX_FILE, "secret"));

		verify(certificateStoragePort).save(any(DigitalCertificate.class));
	}

	@Test
	@DisplayName("Rejects the A3 certificate type")
	void shouldRejectA3CertificateType() {
		final CompanyId id = CompanyId.of(UUID.randomUUID());
		when(companyRepositoryPort.findById(id)).thenReturn(Optional.of(existingCompany(id)));

		assertThatThrownBy(() -> service().execute(new UploadDigitalCertificateCommand(id, "A3", PFX_FILE, "secret")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("A1");

		verify(certificateStoragePort, never()).save(any());
	}

	@Test
	@DisplayName("Throws when the company does not exist")
	void shouldThrowWhenCompanyDoesNotExist() {
		final CompanyId id = CompanyId.of(UUID.randomUUID());
		when(companyRepositoryPort.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().execute(new UploadDigitalCertificateCommand(id, "A1", PFX_FILE, "secret")))
				.isInstanceOf(CompanyNotFoundException.class);

		verify(certificateStoragePort, never()).save(any());
	}

	@Test
	@DisplayName("Propagates the business rule exception when the PFX is invalid")
	void shouldPropagateBusinessRuleExceptionWhenPfxIsInvalid() {
		final CompanyId id = CompanyId.of(UUID.randomUUID());
		when(companyRepositoryPort.findById(id)).thenReturn(Optional.of(existingCompany(id)));
		when(certificateReaderPort.readExpiryDate(PFX_FILE, "wrong-password"))
				.thenThrow(new BusinessRuleException("Invalid certificate file or wrong password"));

		assertThatThrownBy(
				() -> service().execute(new UploadDigitalCertificateCommand(id, "A1", PFX_FILE, "wrong-password")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Invalid certificate file");

		verify(certificateStoragePort, never()).save(any());
	}
}
