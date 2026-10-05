package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.finance.BankIntegration;
import br.gravita.core.domain.finance.BankIntegrationUnavailableException;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.PayableOrigin;
import br.gravita.core.domain.finance.PayableStatus;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementId;
import br.gravita.core.domain.finance.SettlementMethod;
import br.gravita.core.ports.inbound.finance.BankReturnImportResult;
import br.gravita.core.ports.inbound.finance.ConfirmBatchPaymentCommand;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.BankReturnLine;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import br.gravita.core.usercases.finance.ConfirmBatchPaymentService;
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
class ConfirmBatchPaymentServiceTest {

	private static final LocalDate PAID_AT = LocalDate.of(2026, 9, 25);

	@Mock
	private BankIntegrationPort bankIntegrationPort;

	@Mock
	private PayableRepositoryPort payableRepositoryPort;

	@Mock
	private SettlementRepositoryPort settlementRepositoryPort;

	@InjectMocks
	private ConfirmBatchPaymentService service;

	private final ConfirmBatchPaymentCommand command = new ConfirmBatchPaymentCommand(BankIntegration.ITAU, "cnab");

	private Payable payable(final PayableStatus status) {
		return Payable.builder()
				.id(PayableId.of(UUID.randomUUID()))
				.supplierId(UUID.randomUUID())
				.origin(PayableOrigin.MANUAL)
				.amount(new BigDecimal("100.00"))
				.dueDate(LocalDate.now().plusDays(5))
				.costCenterSplit(null)
				.status(status)
				.purchaseReceiptRef(null)
				.installmentNumber(null)
				.installments(null)
				.build();
	}

	private BankReturnLine paidLine(final int lineNumber, final Payable payable, final String amount) {
		return paidLine(lineNumber, payable.getId().value().toString(), amount);
	}

	private BankReturnLine paidLine(final int lineNumber, final String titleIdentifier, final String amount) {
		return new BankReturnLine(lineNumber, titleIdentifier, true, new BigDecimal(amount), null, null, null, null,
				PAID_AT);
	}

	private void returnLines(final BankReturnLine... lines) {
		when(bankIntegrationPort.parseReturnFile(BankIntegration.ITAU, "cnab")).thenReturn(List.of(lines));
	}

	private void found(final Payable payable, final Settlement... previous) {
		when(payableRepositoryPort.findById(payable.getId())).thenReturn(Optional.of(payable));
		when(settlementRepositoryPort.findByPayableId(payable.getId())).thenReturn(List.of(previous));
	}

	private Settlement previousCnabPayment(final Payable payable, final String amount, final LocalDate paidAt) {
		return Settlement.automaticCnabForPayable(SettlementId.of(UUID.randomUUID()), payable.getId(),
				new BigDecimal(amount), null, null, null, null, paidAt.atStartOfDay(ZoneOffset.UTC).toInstant());
	}

	@Test
	@DisplayName("Creates an automatic CNAB settlement linked to the payable and pays it for a confirmed line")
	void confirmedLineCreatesAnAutomaticCnabSettlementLinkedToThePayableAndPaysIt() {
		final Payable payable = payable(PayableStatus.APPROVED);
		returnLines(new BankReturnLine(1, payable.getId().value().toString(), true, new BigDecimal("100.00"),
				new BigDecimal("1.50"), new BigDecimal("2.00"), null, null, PAID_AT));
		found(payable);

		final BankReturnImportResult result = service.execute(command);

		assertThat(result.settledCount()).isEqualTo(1);
		assertThat(result.skippedCount()).isZero();
		assertThat(result.unmatchedLines()).isEmpty();
		assertThat(result.rejectedLines()).isEmpty();
		final ArgumentCaptor<Settlement> settlement = ArgumentCaptor.forClass(Settlement.class);
		verify(settlementRepositoryPort).save(settlement.capture());
		assertThat(settlement.getValue().getMethod()).isEqualTo(SettlementMethod.AUTOMATIC_CNAB);
		assertThat(settlement.getValue().getPayableId()).isEqualTo(payable.getId());
		assertThat(settlement.getValue().getReceivableId()).isNull();
		assertThat(settlement.getValue().getAmount()).isEqualByComparingTo("100.00");
		assertThat(settlement.getValue().getInterest()).isEqualByComparingTo("1.50");
		assertThat(settlement.getValue().getFine()).isEqualByComparingTo("2.00");
		assertThat(settlement.getValue().getTimestamp()).isEqualTo(PAID_AT.atStartOfDay(ZoneOffset.UTC).toInstant());
		final ArgumentCaptor<Payable> saved = ArgumentCaptor.forClass(Payable.class);
		verify(payableRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getId()).isEqualTo(payable.getId());
		assertThat(saved.getValue().getStatus()).isEqualTo(PayableStatus.PAID);
	}

	@Test
	@DisplayName("Counts a discount toward clearing the payable")
	void discountCountsTowardsClearingThePayable() {
		final Payable payable = payable(PayableStatus.APPROVED);
		returnLines(new BankReturnLine(1, payable.getId().value().toString(), true, new BigDecimal("90.00"), null,
				null, new BigDecimal("10.00"), null, PAID_AT));
		found(payable);

		final BankReturnImportResult result = service.execute(command);

		assertThat(result.settledCount()).isEqualTo(1);
		final ArgumentCaptor<Payable> saved = ArgumentCaptor.forClass(Payable.class);
		verify(payableRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getStatus()).isEqualTo(PayableStatus.PAID);
	}

	@Test
	@DisplayName("Leaves the payable approved and reports the line as rejected when the bank rejected it")
	void lineTheBankRejectedLeavesThePayableApprovedAndIsReportedAsRejected() {
		final Payable payable = payable(PayableStatus.APPROVED);
		returnLines(new BankReturnLine(4, payable.getId().value().toString(), false, null, null, null, null, null,
				null, "Insufficient funds"));

		final BankReturnImportResult result = service.execute(command);

		assertThat(result.settledCount()).isZero();
		assertThat(result.skippedCount()).isZero();
		assertThat(result.unmatchedLines()).isEmpty();
		assertThat(result.rejectedLines()).singleElement().satisfies(line -> {
			assertThat(line.lineNumber()).isEqualTo(4);
			assertThat(line.titleIdentifier()).isEqualTo(payable.getId().value().toString());
			assertThat(line.reason()).isEqualTo("Insufficient funds");
		});
		verify(settlementRepositoryPort, never()).save(any());
		verify(payableRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Keeps processing the remaining lines when one line is rejected")
	void rejectionDoesNotStopTheOtherLinesOfTheFile() {
		final Payable rejected = payable(PayableStatus.APPROVED);
		final Payable confirmed = payable(PayableStatus.APPROVED);
		returnLines(
				new BankReturnLine(1, rejected.getId().value().toString(), false, null, null, null, null, null, null,
						"Invalid account"),
				paidLine(2, confirmed, "100.00"));
		found(confirmed);

		final BankReturnImportResult result = service.execute(command);

		assertThat(result.settledCount()).isEqualTo(1);
		assertThat(result.rejectedLines()).hasSize(1);
		final ArgumentCaptor<Payable> saved = ArgumentCaptor.forClass(Payable.class);
		verify(payableRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getId()).isEqualTo(confirmed.getId());
	}

	@Test
	@DisplayName("Skips lines that are neither paid nor rejected without reporting them")
	void linesThatAreNeitherPaidNorRejectedAreSkippedNotReported() {
		returnLines(new BankReturnLine(1, "any", false, null, null, null, null, null, null));

		final BankReturnImportResult result = service.execute(command);

		assertThat(result.skippedCount()).isEqualTo(1);
		assertThat(result.settledCount()).isZero();
		assertThat(result.unmatchedLines()).isEmpty();
		assertThat(result.rejectedLines()).isEmpty();
		verify(settlementRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Reports a line with no matching payable instead of dropping it")
	void lineWithNoMatchingPayableIsReportedNotDropped() {
		final UUID unknown = UUID.randomUUID();
		returnLines(paidLine(7, unknown.toString(), "100.00"));
		when(payableRepositoryPort.findById(PayableId.of(unknown))).thenReturn(Optional.empty());

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
	@DisplayName("Reports a line whose title identifier is not a payable id")
	void titleIdentifierThatIsNotAPayableIdIsReported() {
		returnLines(paidLine(3, "NOSSO-123", "100.00"));

		final BankReturnImportResult result = service.execute(command);

		assertThat(result.unmatchedLines()).singleElement()
				.satisfies(line -> assertThat(line.titleIdentifier()).isEqualTo("NOSSO-123"));
		verify(payableRepositoryPort, never()).findById(any());
	}

	@Test
	@DisplayName("Reports a payment for a payable that is not approved and changes nothing")
	void paymentForAPayableThatIsNotApprovedIsReportedAndChangesNothing() {
		for (final PayableStatus status : new PayableStatus[] {PayableStatus.OPEN, PayableStatus.PAID,
				PayableStatus.CANCELLED }) {
			final Payable payable = payable(status);
			returnLines(paidLine(1, payable, "100.00"));
			found(payable);

			final BankReturnImportResult result = service.execute(command);

			assertThat(result.settledCount()).isZero();
			assertThat(result.unmatchedLines()).singleElement()
					.satisfies(line -> assertThat(line.reason()).contains(status.name()));
		}
		verify(settlementRepositoryPort, never()).save(any());
		verify(payableRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Settles nothing the second time the same file is imported")
	void importingTheSameFileAgainSettlesNothingTwice() {
		final Payable payable = payable(PayableStatus.PAID);
		returnLines(paidLine(1, payable, "100.00"));
		found(payable, previousCnabPayment(payable, "100.00", PAID_AT));

		final BankReturnImportResult result = service.execute(command);

		assertThat(result.settledCount()).isZero();
		assertThat(result.unmatchedLines()).singleElement()
				.satisfies(line -> assertThat(line.reason()).containsIgnoringCase("already imported"));
		verify(settlementRepositoryPort, never()).save(any());
		verify(payableRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Reports a payment that does not clear the payable and leaves it approved")
	void paymentThatDoesNotClearThePayableIsReportedAndLeavesItApproved() {
		for (final String amount : new String[] {"40.00", "150.00" }) {
			final Payable payable = payable(PayableStatus.APPROVED);
			returnLines(paidLine(1, payable, amount));
			found(payable);

			final BankReturnImportResult result = service.execute(command);

			assertThat(result.settledCount()).isZero();
			assertThat(result.unmatchedLines()).singleElement()
					.satisfies(line -> assertThat(line.reason()).contains("does not match"));
		}
		verify(settlementRepositoryPort, never()).save(any());
		verify(payableRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Reports a line with an invalid amount without stopping the other lines")
	void lineWithAnInvalidAmountIsReportedAndDoesNotStopTheRest() {
		final Payable bad = payable(PayableStatus.APPROVED);
		final Payable good = payable(PayableStatus.APPROVED);
		returnLines(paidLine(1, bad, "0.00"), paidLine(2, good, "100.00"));
		when(payableRepositoryPort.findById(bad.getId())).thenReturn(Optional.of(bad));
		found(good);

		final BankReturnImportResult result = service.execute(command);

		assertThat(result.settledCount()).isEqualTo(1);
		assertThat(result.unmatchedLines()).singleElement()
				.satisfies(line -> assertThat(line.lineNumber()).isEqualTo(1));
		final ArgumentCaptor<Settlement> settlement = ArgumentCaptor.forClass(Settlement.class);
		verify(settlementRepositoryPort).save(settlement.capture());
		assertThat(settlement.getValue().getPayableId()).isEqualTo(good.getId());
	}

	@Test
	@DisplayName("Fails the import when the bank is unavailable")
	void anUnavailableBankFailsTheImport() {
		when(bankIntegrationPort.parseReturnFile(BankIntegration.ITAU, "cnab"))
				.thenThrow(new BankIntegrationUnavailableException("not configured"));

		assertThatThrownBy(() -> service.execute(command)).isInstanceOf(BankIntegrationUnavailableException.class);
		verify(settlementRepositoryPort, never()).save(any());
	}
}
