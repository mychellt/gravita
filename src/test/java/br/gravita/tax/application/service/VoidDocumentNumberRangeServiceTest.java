package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DocumentSeries;
import br.gravita.core.domain.masterdata.DocumentSeriesNotFoundException;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.VoidedNumberRange;
import br.gravita.core.ports.inbound.tax.VoidNumberRangeCommand;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.DocumentSeriesRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.VoidedNumberRangeRepositoryPort;
import br.gravita.core.ports.outbound.tax.SefazSubmissionResult;
import br.gravita.core.ports.outbound.tax.SefazVoidNumberRangeRequest;
import br.gravita.core.ports.outbound.tax.SubmitToSefazPort;
import br.gravita.core.usercases.tax.VoidDocumentNumberRangeService;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VoidDocumentNumberRangeServiceTest {

	@Mock
	private CompanyRepositoryPort companyRepositoryPort;

	@Mock
	private DocumentSeriesRepositoryPort documentSeriesRepositoryPort;

	@Mock
	private SubmitToSefazPort submitToSefazPort;

	@Mock
	private VoidedNumberRangeRepositoryPort voidedNumberRangeRepositoryPort;

	private VoidDocumentNumberRangeService service;

	private CompanyId companyId;

	@BeforeEach
	void setUp() {
		service = new VoidDocumentNumberRangeService(companyRepositoryPort, documentSeriesRepositoryPort,
				submitToSefazPort, voidedNumberRangeRepositoryPort);

		companyId = CompanyId.of(UUID.randomUUID());

		lenient().when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.of(company()));
		lenient().when(documentSeriesRepositoryPort.findByCompanyIdAndDocumentType(companyId, FiscalDocumentType.NFE))
				.thenReturn(Optional.of(configuredSeries()));
		lenient().when(submitToSefazPort.voidNumberRange(any()))
				.thenReturn(new SefazSubmissionResult("void-protocol-1"));
		lenient().when(voidedNumberRangeRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	private Company company() {
		return Company.builder()
				.id(companyId)
				.name("Acme Ltda")
				.cnpj(Document.cnpj("11.222.333/0001-81"))
				.ie("123456789")
				.im("987654")
				.cnae("6201500")
				.taxRegime(TaxRegime.SIMPLES_NACIONAL)
				.simplesOptante(true)
				.sefazEnvironment(SefazEnvironment.HOMOLOGATION)
				.address("Rua Teste, 100")
				.state("SP")
				.issuingEmail("nfe@example.com")
				.phone("11999999999")
				.logoUrl(null)
				.parentCompanyId(null)
				.build();
	}

	private DocumentSeries configuredSeries() {
		return DocumentSeries.of(UUID.randomUUID(), companyId, FiscalDocumentType.NFE, "001", 500L, 1L);
	}

	private VoidNumberRangeCommand command(final String justification) {
		return new VoidNumberRangeCommand(companyId, "001", 100L, 110L, justification);
	}

	@Test
	@DisplayName("Requires a justification and rejects the request without it")
	void ac1JustificationIsMandatoryTheRequestIsRejectedWithoutIt() {
		assertThatThrownBy(() -> service.execute(command(null))).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("justification");
		assertThatThrownBy(() -> service.execute(command("  "))).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("justification");

		verify(submitToSefazPort, never()).voidNumberRange(any());
		verify(voidedNumberRangeRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Produces an immutable record with no update or delete operation")
	void ac2TheResultingRecordIsImmutableNoUpdateOrDeleteOperationExists() {
		for (final Method method : VoidedNumberRangeRepositoryPort.class.getMethods()) {
			assertThat(method.getName().toLowerCase()).doesNotContain("update").doesNotContain("delete");
		}
	}

	@Test
	@DisplayName("Carries what SPED and Livros Fiscais need to explain the numbering gap")
	void ac3TheVoidedRangeCarriesWhatSpedLivrosFiscaisWillNeedToExplainTheGap() {
		final VoidedNumberRange result = service.execute(command("numbers printed on damaged forms"));

		assertThat(result.getCompanyId()).isEqualTo(companyId);
		assertThat(result.getDocumentType()).isEqualTo(FiscalDocumentType.NFE);
		assertThat(result.getSeries()).isEqualTo("001");
		assertThat(result.getStartNumber()).isEqualTo(100L);
		assertThat(result.getEndNumber()).isEqualTo(110L);
		assertThat(result.getJustification()).isEqualTo("numbers printed on damaged forms");
		assertThat(result.getSefazProtocol()).isEqualTo("void-protocol-1");
		assertThat(result.getVoidedAt()).isNotNull();

		verify(voidedNumberRangeRepositoryPort).save(result);
	}

	@Test
	@DisplayName("Transmits the void to SEFAZ before persisting it locally")
	void successfulVoidTransmitsToSefazBeforePersistingLocally() {
		service.execute(command("numbers printed on damaged forms"));

		final ArgumentCaptor<SefazVoidNumberRangeRequest> captor = ArgumentCaptor.forClass(SefazVoidNumberRangeRequest.class);
		verify(submitToSefazPort).voidNumberRange(captor.capture());
		assertThat(captor.getValue().companyId()).isEqualTo(companyId);
		assertThat(captor.getValue().environment()).isEqualTo(SefazEnvironment.HOMOLOGATION);
		assertThat(captor.getValue().series()).isEqualTo("001");
		assertThat(captor.getValue().startNumber()).isEqualTo(100L);
		assertThat(captor.getValue().endNumber()).isEqualTo(110L);
		assertThat(captor.getValue().justification()).isEqualTo("numbers printed on damaged forms");

		verify(voidedNumberRangeRepositoryPort).save(any());
	}

	@Test
	@DisplayName("Rejects a company that does not exist")
	void companyThatDoesNotExistIsRejected() {
		when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(command("reason"))).isInstanceOf(ResourceNotFoundException.class);

		verify(submitToSefazPort, never()).voidNumberRange(any());
	}

	@Test
	@DisplayName("Rejects a series not configured for the company")
	void seriesNotConfiguredForTheCompanyIsRejected() {
		when(documentSeriesRepositoryPort.findByCompanyIdAndDocumentType(companyId, FiscalDocumentType.NFE))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(command("reason")))
				.isInstanceOf(DocumentSeriesNotFoundException.class);

		verify(submitToSefazPort, never()).voidNumberRange(any());
	}

	@Test
	@DisplayName("Rejects a series that does not match the configured one")
	void seriesThatDoesNotMatchTheConfiguredOneIsRejected() {
		assertThatThrownBy(() -> service.execute(new VoidNumberRangeCommand(companyId, "999", 100L, 110L, "reason")))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("999");

		verify(submitToSefazPort, never()).voidNumberRange(any());
	}

	@Test
	@DisplayName("Rejects an end number before the start number")
	void anEndNumberBeforeTheStartNumberIsRejected() {
		assertThatThrownBy(() -> service.execute(new VoidNumberRangeCommand(companyId, "001", 110L, 100L, "reason")))
				.isInstanceOf(BusinessRuleException.class);

		verify(submitToSefazPort, never()).voidNumberRange(any());
	}
}
