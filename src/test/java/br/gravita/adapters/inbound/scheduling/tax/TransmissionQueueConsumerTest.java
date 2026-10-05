package br.gravita.adapters.inbound.scheduling.tax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.PersonRef;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.Cfop;
import br.gravita.core.domain.tax.ItemTaxBreakdown;
import br.gravita.core.domain.tax.NaturezaOperacao;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.domain.tax.NfeItem;
import br.gravita.core.domain.tax.NfeRecipient;
import br.gravita.core.domain.tax.SefazUnavailableException;
import br.gravita.core.domain.tax.TaxCalculationTotals;
import br.gravita.core.domain.tax.TaxLineBreakdown;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.domain.tax.TransmissionQueueEntry;
import br.gravita.core.ports.inbound.tax.TransmitNfeCommand;
import br.gravita.core.ports.inbound.tax.TransmitNfeUseCase;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.tax.TransmissionQueuePort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
class TransmissionQueueConsumerTest {

	private static final String VALID_CNPJ = "11.222.333/0001-81";

	@Mock
	private TransmissionQueuePort transmissionQueuePort;

	@Mock
	private NfeRepositoryPort nfeRepositoryPort;

	@Mock
	private TransmitNfeUseCase transmitNfeUseCase;

	private TransmissionQueueConsumer consumer;

	private NfeDocumentId documentId;

	@BeforeEach
	void setUp() {
		consumer = new TransmissionQueueConsumer(transmissionQueuePort, nfeRepositoryPort, transmitNfeUseCase);
		documentId = NfeDocumentId.of(UUID.randomUUID());
	}

	@Test
	@DisplayName("Reschedules the entry with exponential backoff on a timeout instead of failing it immediately")
	void ac2ATimeoutReschedulesTheEntryWithExponentialBackoffInsteadOfFailingItImmediately() {
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document(false)));
		when(transmitNfeUseCase.execute(any())).thenThrow(new SefazUnavailableException("timeout", null));
		final TransmissionQueueEntry entry = new TransmissionQueueEntry(documentId.value(), 1, Instant.now());

		consumer.processEntry(entry);

		final ArgumentCaptor<Instant> nextRetryCaptor = ArgumentCaptor.forClass(Instant.class);
		verify(transmissionQueuePort).reschedule(eq(documentId.value()), eq(2), nextRetryCaptor.capture());
		assertThat(nextRetryCaptor.getValue()).isAfter(Instant.now());
	}

	@Test
	@DisplayName("Grows the backoff delay exponentially with each attempt up to the configured cap")
	void ac2TheBackoffDelayGrowsExponentiallyWithEachAttemptUpToTheConfiguredCap() {
		assertThat(TransmissionQueueConsumer.backoff(1)).isEqualTo(TransmissionQueueConsumer.BASE_BACKOFF);
		assertThat(TransmissionQueueConsumer.backoff(2)).isEqualTo(TransmissionQueueConsumer.BASE_BACKOFF.multipliedBy(2));
		assertThat(TransmissionQueueConsumer.backoff(3)).isEqualTo(TransmissionQueueConsumer.BASE_BACKOFF.multipliedBy(4));
		assertThat(TransmissionQueueConsumer.backoff(50)).isEqualTo(TransmissionQueueConsumer.MAX_BACKOFF);
	}

	@Test
	@DisplayName("Switches to SVC contingency automatically once the attempt threshold is reached")
	void ac3SwitchesToSvcContingencyAutomaticallyOnceTheAttemptThresholdIsReached() {
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document(false)));
		when(transmitNfeUseCase.execute(any())).thenThrow(new SefazUnavailableException("timeout", null));
		final int attemptsBeforeThisOne = TransmissionQueueConsumer.CONTINGENCY_THRESHOLD_ATTEMPTS - 1;
		final TransmissionQueueEntry entry = new TransmissionQueueEntry(documentId.value(), attemptsBeforeThisOne,
				Instant.now());

		consumer.processEntry(entry);

		final ArgumentCaptor<NfeDocument> savedCaptor = ArgumentCaptor.forClass(NfeDocument.class);
		verify(nfeRepositoryPort).save(savedCaptor.capture());
		assertThat(savedCaptor.getValue().isContingencyMode()).isTrue();
	}

	@Test
	@DisplayName("Does not switch to contingency before the attempt threshold is reached")
	void ac3DoesNotSwitchToContingencyBeforeTheAttemptThresholdIsReached() {
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document(false)));
		when(transmitNfeUseCase.execute(any())).thenThrow(new SefazUnavailableException("timeout", null));
		final int attemptsBeforeThisOne = TransmissionQueueConsumer.CONTINGENCY_THRESHOLD_ATTEMPTS - 2;
		final TransmissionQueueEntry entry = new TransmissionQueueEntry(documentId.value(), attemptsBeforeThisOne,
				Instant.now());

		consumer.processEntry(entry);

		verify(nfeRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Does not switch again a document that is already in contingency mode")
	void ac3ADocumentAlreadyInContingencyModeIsNotSwitchedAgain() {
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document(true)));
		when(transmitNfeUseCase.execute(any())).thenThrow(new SefazUnavailableException("timeout", null));
		final TransmissionQueueEntry entry = new TransmissionQueueEntry(documentId.value(),
				TransmissionQueueConsumer.CONTINGENCY_THRESHOLD_ATTEMPTS, Instant.now());

		consumer.processEntry(entry);

		verify(nfeRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Neither reschedules nor changes contingency mode after a successful transmission")
	void successfulTransmissionNeitherReschedulesNorTouchesContingencyMode() {
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document(false)));
		final TransmissionQueueEntry entry = new TransmissionQueueEntry(documentId.value(), 0, Instant.now());

		consumer.processEntry(entry);

		verify(transmitNfeUseCase).execute(new TransmitNfeCommand(documentId.value()));
		verify(transmissionQueuePort, never()).reschedule(any(), anyInt(), any());
		verify(nfeRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Silently leaves for the other consumer an entry that does not resolve to an NF-e document")
	void anEntryThatDoesNotResolveToAnNfeDocumentIsSilentlyLeftForTheOtherConsumer() {
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.empty());
		final TransmissionQueueEntry entry = new TransmissionQueueEntry(documentId.value(), 0, Instant.now());

		consumer.processEntry(entry);

		verify(transmitNfeUseCase, never()).execute(any());
	}

	private NfeDocument document(final boolean contingencyMode) {
		final UUID productId = UUID.randomUUID();
		final TaxLineBreakdown line = new TaxLineBreakdown(TaxType.ICMS, new BigDecimal("100.00"), new BigDecimal("18"),
				new BigDecimal("18.00"), new BigDecimal("18.00"), false, null);
		final ItemTaxBreakdown breakdown = new ItemTaxBreakdown(0, productId.toString(), List.of(line));
		final NfeItem item = new NfeItem(productId, "Produto Teste", BigDecimal.ONE, new BigDecimal("100.00"),
				BigDecimal.ZERO, breakdown);
		final NfeRecipient recipient = NfeRecipient.of(PersonRef.of(UUID.randomUUID()), VALID_CNPJ, PersonType.COMPANY,
				"Cliente PJ Teste", "123456789", "RJ");
		final TaxCalculationTotals totals = TaxCalculationTotals.from(List.of(item.taxBreakdown()));

		return NfeDocument.builder()
				.id(documentId)
				.issuerCompanyId(CompanyId.of(UUID.randomUUID()))
				.originSalesOrderId(null)
				.naturezaOperacao(NaturezaOperacao.VENDA)
				.cfop(new Cfop("5102"))
				.recipient(recipient)
				.items(List.of(item))
				.freight(BigDecimal.ZERO)
				.insurance(BigDecimal.ZERO)
				.otherExpenses(BigDecimal.ZERO)
				.transport(null)
				.referencedAccessKey(null)
				.additionalInfo(null)
				.taxTotals(totals)
				.status(NfeDocumentStatus.SENT)
				.createdAt(Instant.now())
				.documentSeries("001")
				.documentNumber(42L)
				.accessKey("3".repeat(44))
				.sefazProtocol(null)
				.contingencyMode(contingencyMode)
				.rejectionReason(null)
				.xmlStorageRef(null)
				.danfeStorageRef(null)
				.correctionLetters(List.of())
				.authorizedAt(null)
				.cancellationJustification(null)
				.cancelledAt(null)
				.build();
	}
}
