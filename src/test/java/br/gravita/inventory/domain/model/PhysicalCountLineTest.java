package br.gravita.inventory.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.core.domain.inventory.PhysicalCountLine;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PhysicalCountLineTest {

	@Test
	void aLineWithNoCountedQuantityYetHasNoDivergence() {
		PhysicalCountLine line = new PhysicalCountLine(UUID.randomUUID(), new BigDecimal("10"));

		assertThat(line.hasDivergence()).isFalse();
	}

	@Test
	void aLineWhereCountedMatchesSystemHasNoDivergence() {
		PhysicalCountLine line = new PhysicalCountLine(UUID.randomUUID(), new BigDecimal("10"), new BigDecimal("10"));

		assertThat(line.hasDivergence()).isFalse();
	}

	@Test
	void aLineWhereCountedDiffersFromSystemHasADivergence() {
		PhysicalCountLine line = new PhysicalCountLine(UUID.randomUUID(), new BigDecimal("10"), new BigDecimal("7"));

		assertThat(line.hasDivergence()).isTrue();
		assertThat(line.divergence()).isEqualByComparingTo("-3");
	}
}
