package br.gravita.masterdata.application.service;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DocumentSeries;
import br.gravita.core.domain.masterdata.DocumentSeriesNotFoundException;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.masterdata.ConfigureDocumentSeriesCommand;
import br.gravita.core.ports.outbound.persistence.DocumentSeriesRepositoryPort;
import br.gravita.core.usercases.ConfigureDocumentSeriesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigureDocumentSeriesServiceTest {

	private static final CompanyId COMPANY_ID = CompanyId.of(UUID.randomUUID());

	@Mock
	private DocumentSeriesRepositoryPort documentSeriesRepositoryPort;

	@Test
	void shouldConfigureSeriesOnFirstSetup() {
		ConfigureDocumentSeriesService service = new ConfigureDocumentSeriesService(documentSeriesRepositoryPort);
		DocumentSeries placeholder = DocumentSeries.placeholder(COMPANY_ID, FiscalDocumentType.NFE);
		when(documentSeriesRepositoryPort.findByCompanyIdAndDocumentType(COMPANY_ID, FiscalDocumentType.NFE))
				.thenReturn(Optional.of(placeholder));
		when(documentSeriesRepositoryPort.save(any(DocumentSeries.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new ConfigureDocumentSeriesCommand(COMPANY_ID, FiscalDocumentType.NFE, "001", 1000L));

		ArgumentCaptor<DocumentSeries> captor = ArgumentCaptor.forClass(DocumentSeries.class);
		verify(documentSeriesRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getSeries()).isEqualTo("001");
		assertThat(captor.getValue().getNextNumber()).isEqualTo(1000L);
	}

	@Test
	void shouldRejectDecreasingNextNumberOnceAlreadyConfigured() {
		ConfigureDocumentSeriesService service = new ConfigureDocumentSeriesService(documentSeriesRepositoryPort);
		DocumentSeries configured = DocumentSeries.placeholder(COMPANY_ID, FiscalDocumentType.NFCE).reconfigure("001", 500L);
		when(documentSeriesRepositoryPort.findByCompanyIdAndDocumentType(COMPANY_ID, FiscalDocumentType.NFCE))
				.thenReturn(Optional.of(configured));

		assertThatThrownBy(() -> service.execute(
				new ConfigureDocumentSeriesCommand(COMPANY_ID, FiscalDocumentType.NFCE, "001", 100L)))
				.isInstanceOf(BusinessRuleException.class);

		verify(documentSeriesRepositoryPort, never()).save(any());
	}

	@Test
	void shouldThrowWhenSeriesRowDoesNotExist() {
		ConfigureDocumentSeriesService service = new ConfigureDocumentSeriesService(documentSeriesRepositoryPort);
		when(documentSeriesRepositoryPort.findByCompanyIdAndDocumentType(COMPANY_ID, FiscalDocumentType.NFSE))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(
				new ConfigureDocumentSeriesCommand(COMPANY_ID, FiscalDocumentType.NFSE, "001", 1L)))
				.isInstanceOf(DocumentSeriesNotFoundException.class);
	}
}
