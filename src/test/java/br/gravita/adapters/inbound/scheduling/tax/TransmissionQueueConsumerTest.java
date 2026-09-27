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
	void ac2_aTimeoutReschedulesTheEntryWithExponentialBackoffInsteadOfFailingItImmediately() {
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document(false)));
		when(transmitNfeUseCase.execute(any())).thenThrow(new SefazUnavailableException("timeout", null));
		TransmissionQueueEntry entry = new TransmissionQueueEntry(documentId.value(), 1, Instant.now());

		consumer.processEntry(entry);

		ArgumentCaptor<Instant> nextRetryCaptor = ArgumentCaptor.forClass(Instant.class);
		verify(transmissionQueuePort).reschedule(eq(documentId.value()), eq(2), nextRetryCaptor.capture());
		assertThat(nextRetryCaptor.getValue()).isAfter(Instant.now());
	}

	@Test
	void ac2_theBackoffDelayGrowsExponentiallyWithEachAttemptUpToTheConfiguredCap() {
		assertThat(TransmissionQueueConsumer.backoff(1)).isEqualTo(TransmissionQueueConsumer.BASE_BACKOFF);
		assertThat(TransmissionQueueConsumer.backoff(2)).isEqualTo(TransmissionQueueConsumer.BASE_BACKOFF.multipliedBy(2));
		assertThat(TransmissionQueueConsumer.backoff(3)).isEqualTo(TransmissionQueueConsumer.BASE_BACKOFF.multipliedBy(4));
		assertThat(TransmissionQueueConsumer.backoff(50)).isEqualTo(TransmissionQueueConsumer.MAX_BACKOFF);
	}

	@Test
	void ac3_switchesToSvcContingencyAutomaticallyOnceTheAttemptThresholdIsReached() {
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document(false)));
		when(transmitNfeUseCase.execute(any())).thenThrow(new SefazUnavailableException("timeout", null));
		int attemptsBeforeThisOne = TransmissionQueueConsumer.CONTINGENCY_THRESHOLD_ATTEMPTS - 1;
		TransmissionQueueEntry entry = new TransmissionQueueEntry(documentId.value(), attemptsBeforeThisOne,
				Instant.now());

		consumer.processEntry(entry);

		ArgumentCaptor<NfeDocument> savedCaptor = ArgumentCaptor.forClass(NfeDocument.class);
		verify(nfeRepositoryPort).save(savedCaptor.capture());
		assertThat(savedCaptor.getValue().isContingencyMode()).isTrue();
	}

	@Test
	void ac3_doesNotSwitchToContingencyBeforeTheAttemptThresholdIsReached() {
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document(false)));
		when(transmitNfeUseCase.execute(any())).thenThrow(new SefazUnavailableException("timeout", null));
		int attemptsBeforeThisOne = TransmissionQueueConsumer.CONTINGENCY_THRESHOLD_ATTEMPTS - 2;
		TransmissionQueueEntry entry = new TransmissionQueueEntry(documentId.value(), attemptsBeforeThisOne,
				Instant.now());

		consumer.processEntry(entry);

		verify(nfeRepositoryPort, never()).save(any());
	}

	@Test
	void ac3_aDocumentAlreadyInContingencyModeIsNotSwitchedAgain() {
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document(true)));
		when(transmitNfeUseCase.execute(any())).thenThrow(new SefazUnavailableException("timeout", null));
		TransmissionQueueEntry entry = new TransmissionQueueEntry(documentId.value(),
				TransmissionQueueConsumer.CONTINGENCY_THRESHOLD_ATTEMPTS, Instant.now());

		consumer.processEntry(entry);

		verify(nfeRepositoryPort, never()).save(any());
	}

	@Test
	void aSuccessfulTransmissionNeitherReschedulesNorTouchesContingencyMode() {
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document(false)));
		TransmissionQueueEntry entry = new TransmissionQueueEntry(documentId.value(), 0, Instant.now());

		consumer.processEntry(entry);

		verify(transmitNfeUseCase).execute(new TransmitNfeCommand(documentId.value()));
		verify(transmissionQueuePort, never()).reschedule(any(), anyInt(), any());
		verify(nfeRepositoryPort, never()).save(any());
	}

	@Test
	void anEntryThatDoesNotResolveToAnNfeDocumentIsSilentlyLeftForTheOtherConsumer() {
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.empty());
		TransmissionQueueEntry entry = new TransmissionQueueEntry(documentId.value(), 0, Instant.now());

		consumer.processEntry(entry);

		verify(transmitNfeUseCase, never()).execute(any());
	}

	private NfeDocument document(boolean contingencyMode) {
		UUID productId = UUID.randomUUID();
		TaxLineBreakdown line = new TaxLineBreakdown(TaxType.ICMS, new BigDecimal("100.00"), new BigDecimal("18"),
				new BigDecimal("18.00"), new BigDecimal("18.00"), false, null);
		ItemTaxBreakdown breakdown = new ItemTaxBreakdown(0, productId.toString(), List.of(line));
		NfeItem item = new NfeItem(productId, "Produto Teste", BigDecimal.ONE, new BigDecimal("100.00"),
				BigDecimal.ZERO, breakdown);
		NfeRecipient recipient = NfeRecipient.of(PersonRef.of(UUID.randomUUID()), VALID_CNPJ, PersonType.COMPANY,
				"Cliente PJ Teste", "123456789", "RJ");
		TaxCalculationTotals totals = TaxCalculationTotals.from(List.of(item.taxBreakdown()));

		return NfeDocument.of(documentId, CompanyId.of(UUID.randomUUID()), null, NaturezaOperacao.VENDA,
				new Cfop("5102"), recipient, List.of(item), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null,
				null, null, totals, NfeDocumentStatus.SENT, Instant.now(), "001", 42L, "3".repeat(44), null,
				contingencyMode, null, null, null, List.of(), null, null, null);
	}
}
