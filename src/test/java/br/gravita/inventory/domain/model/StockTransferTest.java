package br.gravita.inventory.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.inventory.StockTransfer;
import br.gravita.core.domain.inventory.StockTransferId;
import br.gravita.core.domain.inventory.StockTransferStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StockTransferTest {

	@Test
	@DisplayName("A newly initiated transfer starts in PENDING status")
	void initiateStartsPending() {
		StockTransfer transfer = StockTransfer.initiate(StockTransferId.of(UUID.randomUUID()), UUID.randomUUID(),
				UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10"));

		assertThat(transfer.getStatus()).isEqualTo(StockTransferStatus.PENDING);
	}

	@Test
	@DisplayName("Confirming a pending transfer moves it to CONFIRMED")
	void confirmMovesToConfirmed() {
		StockTransfer transfer = StockTransfer.initiate(StockTransferId.of(UUID.randomUUID()), UUID.randomUUID(),
				UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10"));

		StockTransfer confirmed = transfer.confirm();

		assertThat(confirmed.getStatus()).isEqualTo(StockTransferStatus.CONFIRMED);
	}

	@Test
	@DisplayName("Confirming an already confirmed transfer is rejected")
	void confirmingTwiceIsRejected() {
		StockTransfer confirmed = StockTransfer.initiate(StockTransferId.of(UUID.randomUUID()), UUID.randomUUID(),
				UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10")).confirm();

		assertThatThrownBy(confirmed::confirm).isInstanceOf(BusinessRuleException.class);
	}
}
