package br.gravita.tax.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.tax.PosSession;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.domain.tax.PosSessionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PosSessionTest {

	@Test
	void openingASessionAlwaysStartsOpenWithTheGivenOpenedAtAndAmount() {
		UUID registerId = UUID.randomUUID();
		UUID operatorId = UUID.randomUUID();
		Instant openedAt = Instant.now();

		PosSession session = PosSession.open(PosSessionId.of(UUID.randomUUID()), registerId, operatorId,
				new BigDecimal("100.00"), openedAt);

		assertThat(session.getStatus()).isEqualTo(PosSessionStatus.OPEN);
		assertThat(session.getRegisterId()).isEqualTo(registerId);
		assertThat(session.getOperatorId()).isEqualTo(operatorId);
		assertThat(session.getOpeningChangeAmount()).isEqualByComparingTo("100.00");
		assertThat(session.getOpenedAt()).isEqualTo(openedAt);
		assertThat(session.getClosedAt()).isNull();
	}

	@Test
	void openingWithANegativeOpeningChangeAmountIsRejected() {
		assertThatThrownBy(() -> PosSession.open(PosSessionId.of(UUID.randomUUID()), UUID.randomUUID(),
				UUID.randomUUID(), new BigDecimal("-1"), Instant.now()))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void openingWithoutAnOpeningChangeAmountIsRejected() {
		assertThatThrownBy(() -> PosSession.open(PosSessionId.of(UUID.randomUUID()), UUID.randomUUID(),
				UUID.randomUUID(), null, Instant.now()))
				.isInstanceOf(BusinessRuleException.class);
	}
}
