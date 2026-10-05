package br.gravita.inventory.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.core.domain.inventory.PhysicalCountLine;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PhysicalCountLineTest {

	@Test
	@DisplayName("A line that has not been counted yet has no divergence")
	void lineWithNoCountedQuantityYetHasNoDivergence() {
		final PhysicalCountLine line = new PhysicalCountLine(UUID.randomUUID(), new BigDecimal("10"));

		assertThat(line.hasDivergence()).isFalse();
	}

	@Test
	@DisplayName("A line whose counted quantity equals the system quantity has no divergence")
	void lineWhereCountedMatchesSystemHasNoDivergence() {
		final PhysicalCountLine line = new PhysicalCountLine(UUID.randomUUID(), new BigDecimal("10"), new BigDecimal("10"));

		assertThat(line.hasDivergence()).isFalse();
	}

	@Test
	@DisplayName("A line whose counted quantity differs from the system quantity has a divergence")
	void lineWhereCountedDiffersFromSystemHasADivergence() {
		final PhysicalCountLine line = new PhysicalCountLine(UUID.randomUUID(), new BigDecimal("10"), new BigDecimal("7"));

		assertThat(line.hasDivergence()).isTrue();
		assertThat(line.divergence()).isEqualByComparingTo("-3");
	}
}
