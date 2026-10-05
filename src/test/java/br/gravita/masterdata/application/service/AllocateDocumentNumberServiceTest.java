package br.gravita.masterdata.application.service;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DocumentNumber;
import br.gravita.core.domain.masterdata.DocumentSeries;
import br.gravita.core.domain.masterdata.DocumentSeriesNotFoundException;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.masterdata.AllocateDocumentNumberCommand;
import br.gravita.core.ports.outbound.persistence.DocumentSeriesRepositoryPort;
import br.gravita.core.usercases.AllocateDocumentNumberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllocateDocumentNumberServiceTest {

	private static final CompanyId COMPANY_ID = CompanyId.of(UUID.randomUUID());
	private static final AllocateDocumentNumberCommand COMMAND =
			new AllocateDocumentNumberCommand(COMPANY_ID, FiscalDocumentType.NFE);

	@Mock
	private DocumentSeriesRepositoryPort documentSeriesRepositoryPort;

	@Test
	@DisplayName("Reserves the current next number and advances the series")
	void shouldReserveCurrentNextNumberAndAdvanceTheSeries() {
		final AllocateDocumentNumberService service = new AllocateDocumentNumberService(documentSeriesRepositoryPort);
		final DocumentSeries configured = DocumentSeries.placeholder(COMPANY_ID, FiscalDocumentType.NFE).reconfigure("001", 500L);
		when(documentSeriesRepositoryPort.findByCompanyIdAndDocumentType(COMPANY_ID, FiscalDocumentType.NFE))
				.thenReturn(Optional.of(configured));
		when(documentSeriesRepositoryPort.save(any(DocumentSeries.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final DocumentNumber allocated = service.execute(COMMAND);

		assertThat(allocated.series()).isEqualTo("001");
		assertThat(allocated.number()).isEqualTo(500L);

		final ArgumentCaptor<DocumentSeries> captor = ArgumentCaptor.forClass(DocumentSeries.class);
		verify(documentSeriesRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getNextNumber()).isEqualTo(501L);
	}

	@Test
	@DisplayName("Retries internally on an optimistic lock conflict without surfacing it")
	void shouldRetryInternallyOnOptimisticLockConflictWithoutSurfacingIt() {
		final AllocateDocumentNumberService service = new AllocateDocumentNumberService(documentSeriesRepositoryPort);
		final DocumentSeries configured = DocumentSeries.placeholder(COMPANY_ID, FiscalDocumentType.NFE).reconfigure("001", 500L);
		when(documentSeriesRepositoryPort.findByCompanyIdAndDocumentType(COMPANY_ID, FiscalDocumentType.NFE))
				.thenReturn(Optional.of(configured));
		when(documentSeriesRepositoryPort.save(any(DocumentSeries.class)))
				.thenThrow(new ObjectOptimisticLockingFailureException(DocumentSeries.class, configured.getId()))
				.thenThrow(new ObjectOptimisticLockingFailureException(DocumentSeries.class, configured.getId()))
				.thenAnswer(invocation -> invocation.getArgument(0));

		final DocumentNumber allocated = service.execute(COMMAND);

		assertThat(allocated.number()).isEqualTo(500L);
		verify(documentSeriesRepositoryPort, times(3)).findByCompanyIdAndDocumentType(COMPANY_ID, FiscalDocumentType.NFE);
		verify(documentSeriesRepositoryPort, times(3)).save(any(DocumentSeries.class));
	}

	@Test
	@DisplayName("Gives up after exhausting retries under a persistent conflict")
	void shouldGiveUpAfterExhaustingRetriesUnderPersistentConflict() {
		final AllocateDocumentNumberService service = new AllocateDocumentNumberService(documentSeriesRepositoryPort);
		final DocumentSeries configured = DocumentSeries.placeholder(COMPANY_ID, FiscalDocumentType.NFE).reconfigure("001", 500L);
		when(documentSeriesRepositoryPort.findByCompanyIdAndDocumentType(COMPANY_ID, FiscalDocumentType.NFE))
				.thenReturn(Optional.of(configured));
		when(documentSeriesRepositoryPort.save(any(DocumentSeries.class)))
				.thenThrow(new ObjectOptimisticLockingFailureException(DocumentSeries.class, configured.getId()));

		assertThatThrownBy(() -> service.execute(COMMAND))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);
	}

	@Test
	@DisplayName("Throws when the series row does not exist")
	void shouldThrowWhenSeriesRowDoesNotExist() {
		final AllocateDocumentNumberService service = new AllocateDocumentNumberService(documentSeriesRepositoryPort);
		when(documentSeriesRepositoryPort.findByCompanyIdAndDocumentType(COMPANY_ID, FiscalDocumentType.NFE))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(COMMAND))
				.isInstanceOf(DocumentSeriesNotFoundException.class);
	}

	@Test
	@DisplayName("Rejects allocation when the series is not yet configured")
	void shouldRejectAllocationWhenSeriesNotYetConfigured() {
		final AllocateDocumentNumberService service = new AllocateDocumentNumberService(documentSeriesRepositoryPort);
		final DocumentSeries placeholder = DocumentSeries.placeholder(COMPANY_ID, FiscalDocumentType.NFE);
		when(documentSeriesRepositoryPort.findByCompanyIdAndDocumentType(COMPANY_ID, FiscalDocumentType.NFE))
				.thenReturn(Optional.of(placeholder));

		assertThatThrownBy(() -> service.execute(COMMAND))
				.isInstanceOf(BusinessRuleException.class);
	}
}
