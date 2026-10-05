package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.finance.BankStatementLine;
import br.gravita.core.domain.finance.CashFlowFilter;
import br.gravita.core.domain.finance.CashMovement;
import br.gravita.core.domain.finance.CashMovementDirection;
import br.gravita.core.domain.finance.CashMovementId;
import br.gravita.core.domain.finance.InternalCashBoxId;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementId;
import br.gravita.core.domain.finance.SettlementMethod;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.ReconcileBankStatementCommand;
import br.gravita.core.ports.inbound.finance.ReconciliationResult;
import br.gravita.core.ports.outbound.finance.ImportBankStatementPort;
import br.gravita.core.ports.outbound.persistence.finance.InternalCashBoxRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import br.gravita.core.usercases.finance.ReconcileBankStatementService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReconcileBankStatementServiceTest {

	private static final LocalDate DAY = LocalDate.of(2026, 9, 25);
	private static final Instant NOON = Instant.parse("2026-09-25T12:00:00Z");

	@Mock
	private ImportBankStatementPort importBankStatementPort;

	@Mock
	private SettlementRepositoryPort settlementRepositoryPort;

	@Mock
	private InternalCashBoxRepositoryPort internalCashBoxRepositoryPort;

	@InjectMocks
	private ReconcileBankStatementService service;

	private final UUID bankAccount = UUID.randomUUID();
	private final ReconcileBankStatementCommand command = new ReconcileBankStatementCommand(bankAccount, "statement");

	private static BankStatementLine line(final int number, final LocalDate date, final String amount) {
		return BankStatementLine.unmatched(number, date, new BigDecimal(amount), "entry " + number, null);
	}

	private static Settlement receivableSettlement(final String amount, final Instant at) {
		return Settlement.of(SettlementId.of(UUID.randomUUID()), ReceivableId.of(UUID.randomUUID()),
				new BigDecimal(amount), null, null, null, null, SettlementMethod.AUTOMATIC_CNAB, at);
	}

	private static Settlement payableSettlement(final String amount, final Instant at) {
		return Settlement.ofPayable(SettlementId.of(UUID.randomUUID()), PayableId.of(UUID.randomUUID()),
				new BigDecimal(amount), null, null, null, null, SettlementMethod.MANUAL, at);
	}

	private static CashMovement movement(final CashMovementDirection direction, final String amount, final Instant at) {
		return CashMovement.of(CashMovementId.of(UUID.randomUUID()), InternalCashBoxId.MAIN, direction,
				new BigDecimal(amount), "transfer", at);
	}

	private void statement(final BankStatementLine... lines) {
		when(importBankStatementPort.parse("statement")).thenReturn(List.of(lines));
	}

	private void settlements(final Settlement... settlements) {
		when(settlementRepositoryPort.findRealizedBetween(any(), any(), any())).thenReturn(List.of(settlements));
	}

	private void movements(final CashMovement... movements) {
		when(internalCashBoxRepositoryPort.findMovementsBetween(any(), any())).thenReturn(List.of(movements));
	}

	@Test
	@DisplayName("Matches a credit line to the receivable settlement of the same value and date")
	void creditMatchesTheReceivableSettlementOfTheSameValueAndDate() {
		final Settlement received = receivableSettlement("100.00", NOON);
		statement(line(1, DAY, "100.00"));
		settlements(received);
		movements();

		final ReconciliationResult result = service.execute(command);

		assertThat(result.unmatched()).isEmpty();
		assertThat(result.matched()).singleElement().satisfies(matched -> {
			assertThat(matched.isMatched()).isTrue();
			assertThat(matched.getSettlementId()).isEqualTo(received.getId());
			assertThat(matched.getCashMovementId()).isNull();
		});
	}

	@Test
	@DisplayName("Matches a debit line to the payable settlement of the same value and date")
	void debitMatchesThePayableSettlementOfTheSameValueAndDate() {
		final Settlement paid = payableSettlement("80.00", NOON);
		statement(line(1, DAY, "-80.00"));
		settlements(paid);
		movements();

		final ReconciliationResult result = service.execute(command);

		assertThat(result.matched()).singleElement()
				.satisfies(matched -> assertThat(matched.getSettlementId()).isEqualTo(paid.getId()));
	}

	@Test
	@DisplayName("Requires the line's sign to agree with the direction of the settlement")
	void theSignOfTheLineMustAgreeWithTheDirectionOfTheSettlement() {
		statement(line(1, DAY, "-100.00"), line(2, DAY, "80.00"));
		settlements(receivableSettlement("100.00", NOON), payableSettlement("80.00", NOON));
		movements();

		final ReconciliationResult result = service.execute(command);

		assertThat(result.matched()).isEmpty();
		assertThat(result.unmatched()).extracting(BankStatementLine::getLineNumber).containsExactly(1, 2);
	}

	@Test
	@DisplayName("Counts what was paid on top of the principal toward the matching value")
	void whatWasPaidOnTopOfThePrincipalCountsTowardsTheValue() {
		final Settlement withInterest = Settlement.manual(SettlementId.of(UUID.randomUUID()),
				ReceivableId.of(UUID.randomUUID()), new BigDecimal("100.00"), new BigDecimal("2.50"),
				new BigDecimal("1.00"), new BigDecimal("50.00"), new BigDecimal("0.50"), NOON);
		statement(line(1, DAY, "104.0"));
		settlements(withInterest);
		movements();

		assertThat(service.execute(command).matched()).hasSize(1);
	}

	@Test
	@DisplayName("Matches a deposit to a to-bank movement and a withdrawal to a from-bank movement")
	void depositMatchesAToBankMovementAndAWithdrawalAFromBankMovement() {
		final CashMovement deposit = movement(CashMovementDirection.TO_BANK, "300.00", NOON);
		final CashMovement withdrawal = movement(CashMovementDirection.FROM_BANK, "50.00", NOON);
		statement(line(1, DAY, "300.00"), line(2, DAY, "-50.00"));
		settlements();
		movements(deposit, withdrawal);

		final ReconciliationResult result = service.execute(command);

		assertThat(result.unmatched()).isEmpty();
		assertThat(result.matched()).extracting(BankStatementLine::getCashMovementId)
				.containsExactly(deposit.getId(), withdrawal.getId());
		assertThat(result.matched()).allSatisfy(matched -> assertThat(matched.getSettlementId()).isNull());
	}

	@Test
	@DisplayName("Reports a line without a counterpart as unmatched instead of dropping it")
	void lineWithoutACounterpartIsReportedAsUnmatchedNotDropped() {
		statement(line(1, DAY, "100.00"), line(2, DAY, "999.99"), line(3, DAY.plusDays(1), "100.00"));
		settlements(receivableSettlement("100.00", NOON));
		movements();

		final ReconciliationResult result = service.execute(command);

		assertThat(result.matched()).extracting(BankStatementLine::getLineNumber).containsExactly(1);
		assertThat(result.unmatched()).extracting(BankStatementLine::getLineNumber).containsExactly(2, 3);
		assertThat(result.unmatched()).noneMatch(BankStatementLine::isMatched);
	}

	@Test
	@DisplayName("Lets a counterpart be taken by at most one line")
	void counterpartIsTakenByAtMostOneLine() {
		final Settlement only = receivableSettlement("100.00", NOON);
		statement(line(1, DAY, "100.00"), line(2, DAY, "100.00"));
		settlements(only);
		movements();

		final ReconciliationResult result = service.execute(command);

		assertThat(result.matched()).extracting(BankStatementLine::getLineNumber).containsExactly(1);
		assertThat(result.unmatched()).extracting(BankStatementLine::getLineNumber).containsExactly(2);
	}

	@Test
	@DisplayName("Matches identical lines to the oldest counterparts in order")
	void identicalLinesAreMatchedToTheOldestCounterpartsInOrder() {
		final Settlement first = receivableSettlement("100.00", NOON);
		final Settlement second = receivableSettlement("100.00", NOON.plusSeconds(60));
		statement(line(1, DAY, "100.00"), line(2, DAY, "100.0"));
		settlements(first, second);
		movements();

		final ReconciliationResult result = service.execute(command);

		assertThat(result.matched()).extracting(BankStatementLine::getSettlementId)
				.containsExactly(first.getId(), second.getId());
	}

	@Test
	@DisplayName("Prefers a settlement over a cash movement of the same value and date")
	void settlementIsPreferredOverACashMovementOfTheSameValueAndDate() {
		final Settlement settlement = receivableSettlement("100.00", NOON);
		final CashMovement deposit = movement(CashMovementDirection.TO_BANK, "100.00", NOON);
		statement(line(1, DAY, "100.00"), line(2, DAY, "100.00"));
		settlements(settlement);
		movements(deposit);

		final ReconciliationResult result = service.execute(command);

		assertThat(result.matched()).extracting(BankStatementLine::getSettlementId, BankStatementLine::getCashMovementId)
				.containsExactly(tuple(settlement.getId(), null),
						tuple(null, deposit.getId()));
	}

	@Test
	@DisplayName("Compares dates on the UTC calendar day")
	void datesAreComparedOnTheUtcCalendarDay() {
		final Settlement lateEvening = receivableSettlement("10.00", Instant.parse("2026-09-25T23:59:59Z"));
		final Settlement justAfterMidnight = receivableSettlement("20.00", Instant.parse("2026-09-26T00:00:00Z"));
		statement(line(1, DAY, "10.00"), line(2, DAY, "20.00"));
		settlements(lateEvening, justAfterMidnight);
		movements();

		final ReconciliationResult result = service.execute(command);

		assertThat(result.matched()).extracting(BankStatementLine::getLineNumber).containsExactly(1);
		assertThat(result.unmatched()).extracting(BankStatementLine::getLineNumber).containsExactly(2);
	}

	@Test
	@DisplayName("Looks up the bank account's settlements across the whole statement period")
	void looksUpTheSettlementsOfTheBankAccountAcrossTheWholeStatementPeriod() {
		statement(line(1, DAY, "1.00"), line(2, DAY.plusDays(4), "2.00"), line(3, DAY.plusDays(2), "3.00"));
		settlements();
		movements();

		service.execute(command);

		final ArgumentCaptor<CashFlowFilter> filter = ArgumentCaptor.forClass(CashFlowFilter.class);
		verify(settlementRepositoryPort).findRealizedBetween(eq(Instant.parse("2026-09-25T00:00:00Z")),
				eq(Instant.parse("2026-09-30T00:00:00Z")), filter.capture());
		assertThat(filter.getValue()).isEqualTo(new CashFlowFilter(null, null, bankAccount, null));
		verify(internalCashBoxRepositoryPort).findMovementsBetween(Instant.parse("2026-09-25T00:00:00Z"),
				Instant.parse("2026-09-30T00:00:00Z"));
	}

	@Test
	@DisplayName("Reconciles nothing for an empty statement without querying the ledger")
	void anEmptyStatementReconcilesNothingWithoutQueryingTheLedger() {
		statement();

		final ReconciliationResult result = service.execute(command);

		assertThat(result.matched()).isEmpty();
		assertThat(result.unmatched()).isEmpty();
		verifyNoInteractions(settlementRepositoryPort, internalCashBoxRepositoryPort);
	}

	@Test
	@DisplayName("Rejects an invalid statement before querying anything")
	void anInvalidStatementIsRejectedBeforeAnythingIsQueried() {
		when(importBankStatementPort.parse("statement")).thenThrow(new BusinessRuleException("not a statement"));

		assertThatThrownBy(() -> service.execute(command)).isInstanceOf(BusinessRuleException.class)
				.hasMessage("not a statement");

		verifyNoInteractions(settlementRepositoryPort, internalCashBoxRepositoryPort);
	}

	@Test
	@DisplayName("Never writes anything while reconciling")
	void reconcilingNeverWritesAnything() {
		statement(line(1, DAY, "100.00"));
		settlements(receivableSettlement("100.00", NOON));
		movements();

		service.execute(command);

		verify(settlementRepositoryPort, never()).save(any());
		verify(internalCashBoxRepositoryPort, never()).saveMovement(any());
		verify(internalCashBoxRepositoryPort, never()).save(any());
	}
}
