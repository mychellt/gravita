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

	private Company existingCompany(CompanyId id) {
		return Company.of(id, Document.cnpj("11222333000181"), "123456789", "987654", "6201-5/01",
				TaxRegime.SIMPLES_NACIONAL, true, SefazEnvironment.HOMOLOGATION, "Rua Teste, 100",
				"fiscal@empresa.com", "11999999999", null, null);
	}

	@Test
	void shouldUploadA1CertificateAndPersistExtractedExpiry() {
		CompanyId id = CompanyId.of(UUID.randomUUID());
		Instant expiresAt = Instant.now().plus(365, ChronoUnit.DAYS);
		when(companyRepositoryPort.findById(id)).thenReturn(Optional.of(existingCompany(id)));
		when(certificateReaderPort.readExpiryDate(PFX_FILE, "secret")).thenReturn(expiresAt);
		when(certificateStoragePort.save(any(DigitalCertificate.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service().execute(new UploadDigitalCertificateCommand(id, "A1", PFX_FILE, "secret"));

		ArgumentCaptor<DigitalCertificate> captor = ArgumentCaptor.forClass(DigitalCertificate.class);
		verify(certificateStoragePort).save(captor.capture());
		DigitalCertificate saved = captor.getValue();
		assertThat(saved.getCompanyId()).isEqualTo(id);
		assertThat(saved.getExpiresAt()).isEqualTo(expiresAt);
		assertThat(saved.getPfxPayload()).isEqualTo(PFX_FILE);
	}

	@Test
	void shouldDefaultToA1WhenTypeIsNotProvided() {
		CompanyId id = CompanyId.of(UUID.randomUUID());
		when(companyRepositoryPort.findById(id)).thenReturn(Optional.of(existingCompany(id)));
		when(certificateReaderPort.readExpiryDate(eq(PFX_FILE), eq("secret")))
				.thenReturn(Instant.now().plus(1, ChronoUnit.DAYS));
		when(certificateStoragePort.save(any(DigitalCertificate.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service().execute(new UploadDigitalCertificateCommand(id, null, PFX_FILE, "secret"));

		verify(certificateStoragePort).save(any(DigitalCertificate.class));
	}

	@Test
	void shouldRejectA3CertificateType() {
		CompanyId id = CompanyId.of(UUID.randomUUID());
		when(companyRepositoryPort.findById(id)).thenReturn(Optional.of(existingCompany(id)));

		assertThatThrownBy(() -> service().execute(new UploadDigitalCertificateCommand(id, "A3", PFX_FILE, "secret")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("A1");

		verify(certificateStoragePort, never()).save(any());
	}

	@Test
	void shouldThrowWhenCompanyDoesNotExist() {
		CompanyId id = CompanyId.of(UUID.randomUUID());
		when(companyRepositoryPort.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().execute(new UploadDigitalCertificateCommand(id, "A1", PFX_FILE, "secret")))
				.isInstanceOf(CompanyNotFoundException.class);

		verify(certificateStoragePort, never()).save(any());
	}

	@Test
	void shouldPropagateBusinessRuleExceptionWhenPfxIsInvalid() {
		CompanyId id = CompanyId.of(UUID.randomUUID());
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
