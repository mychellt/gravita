package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.CompanyNotFoundException;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.VoidedNumberRange;
import br.gravita.core.ports.inbound.tax.VoidNumberRangeCommand;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.VoidedNumberRangeRepositoryPort;
import br.gravita.core.ports.outbound.tax.SefazSubmissionResult;
import br.gravita.core.ports.outbound.tax.SefazVoidRangeRequest;
import br.gravita.core.ports.outbound.tax.SubmitToSefazPort;
import br.gravita.core.usercases.tax.VoidDocumentNumberRangeService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
	private SubmitToSefazPort submitToSefazPort;

	@Mock
	private VoidedNumberRangeRepositoryPort voidedNumberRangeRepositoryPort;

	private VoidDocumentNumberRangeService service;

	private CompanyId companyId;

	@BeforeEach
	void setUp() {
		service = new VoidDocumentNumberRangeService(companyRepositoryPort, submitToSefazPort,
				voidedNumberRangeRepositoryPort);

		companyId = CompanyId.of(UUID.randomUUID());

		lenient().when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.of(company()));
		lenient().when(submitToSefazPort.voidRange(any())).thenReturn(new SefazSubmissionResult("void-protocol-1"));
		lenient().when(voidedNumberRangeRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	private Company company() {
		return Company.of(companyId, Document.cnpj("11.222.333/0001-81"), "123456789", "987654", "6201500",
				TaxRegime.SIMPLES_NACIONAL, true, SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP",
				"nfe@example.com", "11999999999", null, null);
	}

	private VoidNumberRangeCommand command() {
		return new VoidNumberRangeCommand(companyId.value(), "001", 100L, 110L, "duplicate numbering skipped");
	}

	@Test
	void ac1_aRequestWithoutJustificationIsRejected() {
		VoidNumberRangeCommand command = new VoidNumberRangeCommand(companyId.value(), "001", 100L, 110L, null);

		assertThatThrownBy(() -> service.execute(command)).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("justification");

		verify(submitToSefazPort, never()).voidRange(any());
		verify(voidedNumberRangeRepositoryPort, never()).save(any());
	}

	@Test
	void ac1_aRequestWithABlankJustificationIsRejected() {
		VoidNumberRangeCommand command = new VoidNumberRangeCommand(companyId.value(), "001", 100L, 110L, "   ");

		assertThatThrownBy(() -> service.execute(command)).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("justification");

		verify(submitToSefazPort, never()).voidRange(any());
	}

	@Test
	void aRangeWhoseStartIsAfterItsEndIsRejected() {
		VoidNumberRangeCommand command = new VoidNumberRangeCommand(companyId.value(), "001", 110L, 100L, "oops");

		assertThatThrownBy(() -> service.execute(command)).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("startNumber");

		verify(submitToSefazPort, never()).voidRange(any());
	}

	@Test
	void aNonExistentCompanyIsRejected() {
		when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(command())).isInstanceOf(CompanyNotFoundException.class);

		verify(submitToSefazPort, never()).voidRange(any());
	}

	@Test
	void successfullyVoidingARangeTransmitsToSefazAndPersistsTheImmutableRecord() {
		VoidedNumberRange result = service.execute(command());

		ArgumentCaptor<SefazVoidRangeRequest> requestCaptor = ArgumentCaptor.forClass(SefazVoidRangeRequest.class);
		verify(submitToSefazPort).voidRange(requestCaptor.capture());
		assertThat(requestCaptor.getValue().companyId()).isEqualTo(companyId);
		assertThat(requestCaptor.getValue().environment()).isEqualTo(SefazEnvironment.HOMOLOGATION);
		assertThat(requestCaptor.getValue().series()).isEqualTo("001");
		assertThat(requestCaptor.getValue().startNumber()).isEqualTo(100L);
		assertThat(requestCaptor.getValue().endNumber()).isEqualTo(110L);
		assertThat(requestCaptor.getValue().justification()).isEqualTo("duplicate numbering skipped");

		ArgumentCaptor<VoidedNumberRange> savedCaptor = ArgumentCaptor.forClass(VoidedNumberRange.class);
		verify(voidedNumberRangeRepositoryPort).save(savedCaptor.capture());
		assertThat(savedCaptor.getValue().getProtocol()).isEqualTo("void-protocol-1");
		assertThat(savedCaptor.getValue().getCompanyId()).isEqualTo(companyId);

		assertThat(result.getProtocol()).isEqualTo("void-protocol-1");
	}
}
