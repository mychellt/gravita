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
import org.junit.jupiter.api.DisplayName;
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

	private Receivable receivable(final ReceivableStatus status) {
		return Receivable.of(ReceivableId.of(UUID.randomUUID()), UUID.randomUUID(), ReceivableOrigin.MANUAL,
				new BigDecimal("100.00"), LocalDate.now().plusDays(30), null, status, null, null);
	}

	private BankReturnLine paidLine(final int lineNumber, final Receivable receivable, final String amount) {
		return paidLine(lineNumber, receivable.getId().value().toString(), amount);
	}

	private BankReturnLine paidLine(final int lineNumber, final String titleIdentifier, final String amount) {
		return new BankReturnLine(lineNumber, titleIdentifier, true, new BigDecimal(amount), null, null, null,
				null, PAID_AT);
	}

	private void returnLines(final BankReturnLine... lines) {
		when(bankIntegrationPort.parseReturnFile(BankIntegration.ITAU, "cnab")).thenReturn(List.of(lines));
	}

	private void found(final Receivable receivable, final Settlement... previous) {
		when(receivableRepositoryPort.findById(receivable.getId())).thenReturn(Optional.of(receivable));
		when(settlementRepositoryPort.findByReceivableId(receivable.getId())).thenReturn(List.of(previous));
	}

	private Settlement previousCnabPayment(final Receivable receivable, final String amount, final LocalDate paidAt) {
		return Settlement.automaticCnab(SettlementId.of(UUID.randomUUID()), receivable.getId(),
				new BigDecimal(amount), null, null, null, null, paidAt.atStartOfDay(ZoneOffset.UTC).toInstant());
	}

	@Test
	@DisplayName("Creates an automatic CNAB settlement and settles the receivable for a line paid in full")
	void paidLineForTheFullAmountCreatesAnAutomaticCnabSettlementAndSettlesTheReceivable() {
		final Receivable receivable = receivable(ReceivableStatus.OPEN);
		returnLines(new BankReturnLine(1, receivable.getId().value().toString(), true, new BigDecimal("100.00"),
				new BigDecimal("1.50"), new BigDecimal("2.00"), null, null, PAID_AT));
		found(receivable);

		final BankReturnImportResult result = service.execute(command);

		assertThat(result.settledCount()).isEqualTo(1);
		assertThat(result.skippedCount()).isZero();
		assertThat(result.unmatchedLines()).isEmpty();
		final ArgumentCaptor<Settlement> settlement = ArgumentCaptor.forClass(Settlement.class);
		verify(settlementRepositoryPort).save(settlement.capture());
		assertThat(settlement.getValue().getMethod()).isEqualTo(SettlementMethod.AUTOMATIC_CNAB);
		assertThat(settlement.getValue().getReceivableId()).isEqualTo(receivable.getId());
		assertThat(settlement.getValue().getAmount()).isEqualByComparingTo("100.00");
		assertThat(settlement.getValue().getInterest()).isEqualByComparingTo("1.50");
		assertThat(settlement.getValue().getFine()).isEqualByComparingTo("2.00");
		assertThat(settlement.getValue().getTimestamp()).isEqualTo(PAID_AT.atStartOfDay(ZoneOffset.UTC).toInstant());
		final ArgumentCaptor<Receivable> saved = ArgumentCaptor.forClass(Receivable.class);
		verify(receivableRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getStatus()).isEqualTo(ReceivableStatus.SETTLED);
	}

	@Test
	@DisplayName("Partially settles the receivable for a line paid below the amount")
	void paidLineBelowTheAmountPartiallySettlesTheReceivable() {
		final Receivable receivable = receivable(ReceivableStatus.OPEN);
		returnLines(paidLine(1, receivable, "40.00"));
		found(receivable);

		final BankReturnImportResult result = service.execute(command);

		assertThat(result.settledCount()).isEqualTo(1);
		final ArgumentCaptor<Receivable> saved = ArgumentCaptor.forClass(Receivable.class);
		verify(receivableRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getStatus()).isEqualTo(ReceivableStatus.PARTIALLY_SETTLED);
	}

	@Test
	@DisplayName("Settles a partially settled receivable with the remainder, counting a discount toward the amount")
	void theRemainderOfAPartiallySettledReceivableSettlesItAndADiscountCountsTowardTheAmount() {
		final Receivable receivable = receivable(ReceivableStatus.PARTIALLY_SETTLED);
		returnLines(new BankReturnLine(1, receivable.getId().value().toString(), true, new BigDecimal("50.00"), null,
				null, new BigDecimal("10.00"), null, PAID_AT));
		found(receivable, previousCnabPayment(receivable, "40.00", PAID_AT.minusDays(7)));

		service.execute(command);

		final ArgumentCaptor<Receivable> saved = ArgumentCaptor.forClass(Receivable.class);
		verify(receivableRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getStatus()).isEqualTo(ReceivableStatus.SETTLED);
	}

	@Test
	@DisplayName("Skips lines that are not payments without reporting them")
	void linesThatAreNotPaymentsAreSkippedNotReported() {
		returnLines(new BankReturnLine(1, "any", false, null, null, null, null, null, null));

		final BankReturnImportResult result = service.execute(command);

		assertThat(result.skippedCount()).isEqualTo(1);
		assertThat(result.settledCount()).isZero();
		assertThat(result.unmatchedLines()).isEmpty();
		verify(settlementRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Reports a line with no matching receivable instead of dropping it")
	void lineWithNoMatchingReceivableIsReportedNotDropped() {
		final UUID unknown = UUID.randomUUID();
		returnLines(paidLine(7, unknown.toString(), "100.00"));
		when(receivableRepositoryPort.findById(ReceivableId.of(unknown))).thenReturn(Optional.empty());

		final BankReturnImportResult result = service.execute(command);

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
	@DisplayName("Reports a line whose title identifier is not a receivable id")
	void titleIdentifierThatIsNotAReceivableIdIsReported() {
		returnLines(paidLine(3, "NOSSO-123", "100.00"));

		final BankReturnImportResult result = service.execute(command);

		assertThat(result.unmatchedLines()).singleElement()
				.satisfies(line -> assertThat(line.titleIdentifier()).isEqualTo("NOSSO-123"));
		verify(receivableRepositoryPort, never()).findById(any());
	}

	@Test
	@DisplayName("Reports a payment for a receivable that is not open and changes nothing")
	void paymentForAReceivableThatIsNotOpenIsReportedAndChangesNothing() {
		for (final ReceivableStatus status : new ReceivableStatus[] {ReceivableStatus.SETTLED,
				ReceivableStatus.CANCELLED, ReceivableStatus.RENEGOTIATED }) {
			final Receivable receivable = receivable(status);
			returnLines(paidLine(1, receivable, "100.00"));
			found(receivable);

			final BankReturnImportResult result = service.execute(command);

			assertThat(result.settledCount()).isZero();
			assertThat(result.unmatchedLines()).singleElement()
					.satisfies(line -> assertThat(line.reason()).contains(status.name()));
		}
		verify(settlementRepositoryPort, never()).save(any());
		verify(receivableRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Settles nothing the second time the same file is imported")
	void importingTheSameFileAgainSettlesNothingTwice() {
		final Receivable receivable = receivable(ReceivableStatus.SETTLED);
		returnLines(paidLine(1, receivable, "100.00"));
		found(receivable, previousCnabPayment(receivable, "100.00", PAID_AT));

		final BankReturnImportResult result = service.execute(command);

		assertThat(result.settledCount()).isZero();
		assertThat(result.unmatchedLines()).singleElement()
				.satisfies(line -> assertThat(line.reason()).containsIgnoringCase("already imported"));
		verify(settlementRepositoryPort, never()).save(any());
		verify(receivableRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Reports a line with an invalid amount without stopping the other lines")
	void lineWithAnInvalidAmountIsReportedAndDoesNotStopTheRest() {
		final Receivable bad = receivable(ReceivableStatus.OPEN);
		final Receivable good = receivable(ReceivableStatus.OPEN);
		returnLines(paidLine(1, bad, "0.00"), paidLine(2, good, "100.00"));
		when(receivableRepositoryPort.findById(bad.getId())).thenReturn(Optional.of(bad));
		found(good);

		final BankReturnImportResult result = service.execute(command);

		assertThat(result.settledCount()).isEqualTo(1);
		assertThat(result.unmatchedLines()).singleElement()
				.satisfies(line -> assertThat(line.lineNumber()).isEqualTo(1));
		final ArgumentCaptor<Settlement> settlement = ArgumentCaptor.forClass(Settlement.class);
		verify(settlementRepositoryPort).save(settlement.capture());
		assertThat(settlement.getValue().getReceivableId()).isEqualTo(good.getId());
	}

	@Test
	@DisplayName("Fails the import when the bank is unavailable")
	void anUnavailableBankFailsTheImport() {
		when(bankIntegrationPort.parseReturnFile(BankIntegration.ITAU, "cnab"))
				.thenThrow(new BankIntegrationUnavailableException("not configured"));

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.execute(command))
				.isInstanceOf(BankIntegrationUnavailableException.class);
		verify(settlementRepositoryPort, never()).save(any());
	}
}
