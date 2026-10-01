package br.gravita.finance.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.core.domain.finance.CashMovementDirection;
import br.gravita.core.domain.finance.DailyClosing;
import br.gravita.core.domain.finance.InternalCashBox;
import br.gravita.core.domain.finance.InternalCashBoxId;
import br.gravita.core.ports.inbound.finance.CloseDailyCashCommand;
import br.gravita.core.ports.inbound.finance.CloseDailyCashUseCase;
import br.gravita.core.ports.inbound.finance.RecordInternalCashMovementCommand;
import br.gravita.core.ports.inbound.finance.RecordInternalCashMovementUseCase;
import br.gravita.core.ports.outbound.persistence.finance.InternalCashBoxRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CloseDailyCashIntegrationTest {

	@Autowired
	private CloseDailyCashUseCase closeDailyCashUseCase;

	@Autowired
	private RecordInternalCashMovementUseCase recordInternalCashMovementUseCase;

	@Autowired
	private InternalCashBoxRepositoryPort internalCashBoxRepositoryPort;

	@Test
	@DisplayName("Closes today's cash from the persisted movements")
	void closesTodayFromThePersistedMovements() {
		internalCashBoxRepositoryPort.save(InternalCashBox.of(InternalCashBoxId.MAIN, new BigDecimal("100.00")));
		recordInternalCashMovementUseCase.execute(new RecordInternalCashMovementCommand(
				CashMovementDirection.FROM_BANK, new BigDecimal("60.00"), "Change float"));
		recordInternalCashMovementUseCase.execute(new RecordInternalCashMovementCommand(
				CashMovementDirection.TO_BANK, new BigDecimal("25.50"), "Deposit"));

		DailyClosing closing = closeDailyCashUseCase
				.execute(new CloseDailyCashCommand(InternalCashBoxId.MAIN, LocalDate.now()));

		assertThat(closing.getOpeningBalance()).isEqualByComparingTo("100.00");
		assertThat(closing.getEntries()).isEqualByComparingTo("60.00");
		assertThat(closing.getExits()).isEqualByComparingTo("25.50");
		assertThat(closing.getClosingBalance()).isEqualByComparingTo("134.50");
		assertThat(closing.getMovements()).hasSize(2);
	}
}
