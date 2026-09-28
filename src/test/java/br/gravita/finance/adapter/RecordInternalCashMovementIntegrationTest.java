package br.gravita.finance.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.core.domain.finance.CashMovement;
import br.gravita.core.domain.finance.CashMovementDirection;
import br.gravita.core.domain.finance.InternalCashBox;
import br.gravita.core.domain.finance.InternalCashBoxId;
import br.gravita.core.ports.inbound.finance.RecordInternalCashMovementCommand;
import br.gravita.core.ports.inbound.finance.RecordInternalCashMovementUseCase;
import br.gravita.core.ports.outbound.persistence.finance.InternalCashBoxRepositoryPort;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RecordInternalCashMovementIntegrationTest {

	@Autowired
	private RecordInternalCashMovementUseCase recordInternalCashMovementUseCase;

	@Autowired
	private InternalCashBoxRepositoryPort internalCashBoxRepositoryPort;

	private void givenCashBoxWithBalance(String balance) {
		internalCashBoxRepositoryPort
				.save(InternalCashBox.of(InternalCashBoxId.MAIN, new BigDecimal(balance)));
	}

	@Test
	void persistsTheBalanceOfTheCashBoxAfterEachMovement() {
		givenCashBoxWithBalance("100.00");

		CashMovement deposit = recordInternalCashMovementUseCase.execute(new RecordInternalCashMovementCommand(
				CashMovementDirection.TO_BANK, new BigDecimal("40.25"), "Deposit of the day's cash"));
		recordInternalCashMovementUseCase.execute(new RecordInternalCashMovementCommand(
				CashMovementDirection.FROM_BANK, new BigDecimal("10.00"), "Change float"));

		assertThat(deposit.getDirection()).isEqualTo(CashMovementDirection.TO_BANK);
		assertThat(deposit.getAmount()).isEqualByComparingTo("40.25");
		assertThat(deposit.getJustification()).isEqualTo("Deposit of the day's cash");
		InternalCashBox box = internalCashBoxRepositoryPort.findById(InternalCashBoxId.MAIN).orElseThrow();
		assertThat(box.getBalance()).isEqualByComparingTo("69.75");
	}
}
