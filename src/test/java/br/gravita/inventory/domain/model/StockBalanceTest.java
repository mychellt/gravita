package br.gravita.inventory.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockBalanceId;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StockBalanceTest {

	@Test
	void availableIsOnHandMinusReserved() {
		StockBalance balance = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), UUID.randomUUID(),
				UUID.randomUUID(), new BigDecimal("100"), new BigDecimal("30"), BigDecimal.ZERO,
				new BigDecimal("12.50"));

		assertThat(balance.available()).isEqualByComparingTo("70");
	}
}
