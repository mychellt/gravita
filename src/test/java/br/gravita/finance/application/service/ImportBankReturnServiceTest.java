package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.finance.BankIntegration;
import br.gravita.core.domain.finance.BankIntegrationUnavailableException;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementId;
import br.gravita.core.domain.finance.SettlementMethod;
import br.gravita.core.ports.inbound.finance.BankReturnImportResult;
import br.gravita.core.ports.inbound.finance.ImportBankReturnCommand;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.BankReturnLine;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import br.gravita.core.usercases.finance.ImportBankReturnService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImportBankReturnServiceTest {

	private static final LocalDate PAID_AT = LocalDate.of(2026, 9, 25);

	@Mock
	private BankIntegrationPort bankIntegrationPort;

	@Mock
	private ReceivableRepositoryPort receivableRepositoryPort;

	@Mock
	private SettlementRepositoryPort settlementRepositoryPort;

	@InjectMocks
	private ImportBankReturnService service;

	private final ImportBankReturnCommand command = new ImportBankReturnCommand(BankIntegration.ITAU, "cnab");

	private Receivable receivable(ReceivableStatus status) {
		return Receivable.of(ReceivableId.of(UUID.randomUUID()), UUID.randomUUID(), ReceivableOrigin.MANUAL,
				new BigDecimal("100.00"), LocalDate.now().plusDays(30), null, status, null, null);
	}

	private BankReturnLine paidLine(int lineNumber, Receivable receivable, String amount) {
		return paidLine(lineNumber, receivable.getId().value().toString(), amount);
	}

	private BankReturnLine paidLine(int lineNumber, String titleIdentifier, String amount) {
		return new BankReturnLine(lineNumber, titleIdentifier, true, new BigDecimal(amount), null, null, null,
				null, PAID_AT);
	}

	private void returnLines(BankReturnLine... lines) {
		when(bankIntegrationPort.parseReturnFile(BankIntegration.ITAU, "cnab")).thenReturn(List.of(lines));
	}

	private void found(Receivable receivable, Settlement... previous) {
		when(receivableRepositoryPort.findById(receivable.getId())).thenReturn(Optional.of(receivable));
		when(settlementRepositoryPort.findByReceivableId(receivable.getId())).thenReturn(List.of(previous));
	}

	private Settlement previousCnabPayment(Receivable receivable, String amount, LocalDate paidAt) {
		return Settlement.automaticCnab(SettlementId.of(UUID.randomUUID()), receivable.getId(),
				new BigDecimal(amount), null, null, null, null, paidAt.atStartOfDay(ZoneOffset.UTC).toInstant());
	}

	@Test
	void aPaidLineForTheFullAmountCreatesAnAutomaticCnabSettlementAndSettlesTheReceivable() {
		Receivable receivable = receivable(ReceivableStatus.OPEN);
		returnLines(new BankReturnLine(1, receivable.getId().value().toString(), true, new BigDecimal("100.00"),
				new BigDecimal("1.50"), new BigDecimal("2.00"), null, null, PAID_AT));
		found(receivable);

		BankReturnImportResult result = service.execute(command);

		assertThat(result.settledCount()).isEqualTo(1);
		assertThat(result.skippedCount()).isZero();
		assertThat(result.unmatchedLines()).isEmpty();
		ArgumentCaptor<Settlement> settlement = ArgumentCaptor.forClass(Settlement.class);
		verify(settlementRepositoryPort).save(settlement.capture());
		assertThat(settlement.getValue().getMethod()).isEqualTo(SettlementMethod.AUTOMATIC_CNAB);
		assertThat(settlement.getValue().getReceivableId()).isEqualTo(receivable.getId());
		assertThat(settlement.getValue().getAmount()).isEqualByComparingTo("100.00");
		assertThat(settlement.getValue().getInterest()).isEqualByComparingTo("1.50");
		assertThat(settlement.getValue().getFine()).isEqualByComparingTo("2.00");
		assertThat(settlement.getValue().getTimestamp()).isEqualTo(PAID_AT.atStartOfDay(ZoneOffset.UTC).toInstant());
		ArgumentCaptor<Receivable> saved = ArgumentCaptor.forClass(Receivable.class);
		verify(receivableRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getStatus()).isEqualTo(ReceivableStatus.SETTLED);
	}

	@Test
	void aPaidLineBelowTheAmountPartiallySettlesTheReceivable() {
		Receivable receivable = receivable(ReceivableStatus.OPEN);
		returnLines(paidLine(1, receivable, "40.00"));
		found(receivable);

		BankReturnImportResult result = service.execute(command);

		assertThat(result.settledCount()).isEqualTo(1);
		ArgumentCaptor<Receivable> saved = ArgumentCaptor.forClass(Receivable.class);
		verify(receivableRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getStatus()).isEqualTo(ReceivableStatus.PARTIALLY_SETTLED);
	}

	@Test
	void theRemainderOfAPartiallySettledReceivableSettlesItAndADiscountCountsTowardTheAmount() {
		Receivable receivable = receivable(ReceivableStatus.PARTIALLY_SETTLED);
		returnLines(new BankReturnLine(1, receivable.getId().value().toString(), true, new BigDecimal("50.00"), null,
				null, new BigDecimal("10.00"), null, PAID_AT));
		found(receivable, previousCnabPayment(receivable, "40.00", PAID_AT.minusDays(7)));

		service.execute(command);

		ArgumentCaptor<Receivable> saved = ArgumentCaptor.forClass(Receivable.class);
		verify(receivableRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getStatus()).isEqualTo(ReceivableStatus.SETTLED);
	}

	@Test
	void linesThatAreNotPaymentsAreSkippedNotReported() {
		returnLines(new BankReturnLine(1, "any", false, null, null, null, null, null, null));

		BankReturnImportResult result = service.execute(command);

		assertThat(result.skippedCount()).isEqualTo(1);
		assertThat(result.settledCount()).isZero();
		assertThat(result.unmatchedLines()).isEmpty();
		verify(settlementRepositoryPort, never()).save(any());
	}

	@Test
	void aLineWithNoMatchingReceivableIsReportedNotDropped() {
		UUID unknown = UUID.randomUUID();
		returnLines(paidLine(7, unknown.toString(), "100.00"));
		when(receivableRepositoryPort.findById(ReceivableId.of(unknown))).thenReturn(Optional.empty());

		BankReturnImportResult result = service.execute(command);

		assertThat(result.settledCount()).isZero();
		assertThat(result.unmatchedLines()).singleElement().satisfies(line -> {
			assertThat(line.lineNumber()).isEqualTo(7);
			assertThat(line.titleIdentifier()).isEqualTo(unknown.toString());
			assertThat(line.amount()).isEqualByComparingTo("100.00");
			assertThat(line.reason()).isNotBlank();
		});
		verify(settlementRepositoryPort, never()).save(any());
	}

	@Test
	void aTitleIdentifierThatIsNotAReceivableIdIsReported() {
		returnLines(paidLine(3, "NOSSO-123", "100.00"));

		BankReturnImportResult result = service.execute(command);

		assertThat(result.unmatchedLines()).singleElement()
				.satisfies(line -> assertThat(line.titleIdentifier()).isEqualTo("NOSSO-123"));
		verify(receivableRepositoryPort, never()).findById(any());
	}

	@Test
	void aPaymentForAReceivableThatIsNotOpenIsReportedAndChangesNothing() {
		for (ReceivableStatus status : new ReceivableStatus[] { ReceivableStatus.SETTLED,
				ReceivableStatus.CANCELLED, ReceivableStatus.RENEGOTIATED }) {
			Receivable receivable = receivable(status);
			returnLines(paidLine(1, receivable, "100.00"));
			found(receivable);

			BankReturnImportResult result = service.execute(command);

			assertThat(result.settledCount()).isZero();
			assertThat(result.unmatchedLines()).singleElement()
					.satisfies(line -> assertThat(line.reason()).contains(status.name()));
		}
		verify(settlementRepositoryPort, never()).save(any());
		verify(receivableRepositoryPort, never()).save(any());
	}

	@Test
	void importingTheSameFileAgainSettlesNothingTwice() {
		Receivable receivable = receivable(ReceivableStatus.SETTLED);
		returnLines(paidLine(1, receivable, "100.00"));
		found(receivable, previousCnabPayment(receivable, "100.00", PAID_AT));

		BankReturnImportResult result = service.execute(command);

		assertThat(result.settledCount()).isZero();
		assertThat(result.unmatchedLines()).singleElement()
				.satisfies(line -> assertThat(line.reason()).containsIgnoringCase("already imported"));
		verify(settlementRepositoryPort, never()).save(any());
		verify(receivableRepositoryPort, never()).save(any());
	}

	@Test
	void aLineWithAnInvalidAmountIsReportedAndDoesNotStopTheRest() {
		Receivable bad = receivable(ReceivableStatus.OPEN);
		Receivable good = receivable(ReceivableStatus.OPEN);
		returnLines(paidLine(1, bad, "0.00"), paidLine(2, good, "100.00"));
		when(receivableRepositoryPort.findById(bad.getId())).thenReturn(Optional.of(bad));
		found(good);

		BankReturnImportResult result = service.execute(command);

		assertThat(result.settledCount()).isEqualTo(1);
		assertThat(result.unmatchedLines()).singleElement()
				.satisfies(line -> assertThat(line.lineNumber()).isEqualTo(1));
		ArgumentCaptor<Settlement> settlement = ArgumentCaptor.forClass(Settlement.class);
		verify(settlementRepositoryPort).save(settlement.capture());
		assertThat(settlement.getValue().getReceivableId()).isEqualTo(good.getId());
	}

	@Test
	void anUnavailableBankFailsTheImport() {
		when(bankIntegrationPort.parseReturnFile(BankIntegration.ITAU, "cnab"))
				.thenThrow(new BankIntegrationUnavailableException("not configured"));

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.execute(command))
				.isInstanceOf(BankIntegrationUnavailableException.class);
		verify(settlementRepositoryPort, never()).save(any());
	}
}
