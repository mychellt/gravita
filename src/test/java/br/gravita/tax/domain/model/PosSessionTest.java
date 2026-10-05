package br.gravita.tax.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.PosSession;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.domain.tax.PosSessionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PosSessionTest {

	@Test
	@DisplayName("Opening a session always starts it open with the given opened-at time and amount")
	void openingASessionAlwaysStartsOpenWithTheGivenOpenedAtAndAmount() {
		final UUID registerId = UUID.randomUUID();
		final UUID operatorId = UUID.randomUUID();
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());
		final Instant openedAt = Instant.now();

		final PosSession session = PosSession.open(PosSessionId.of(UUID.randomUUID()), registerId, operatorId, companyId,
				new BigDecimal("100.00"), openedAt);

		assertThat(session.getStatus()).isEqualTo(PosSessionStatus.OPEN);
		assertThat(session.getRegisterId()).isEqualTo(registerId);
		assertThat(session.getOperatorId()).isEqualTo(operatorId);
		assertThat(session.getCompanyId()).isEqualTo(companyId);
		assertThat(session.getOpeningChangeAmount()).isEqualByComparingTo("100.00");
		assertThat(session.getOpenedAt()).isEqualTo(openedAt);
		assertThat(session.getClosedAt()).isNull();
	}

	@Test
	@DisplayName("Rejects opening with a negative opening change amount")
	void openingWithANegativeOpeningChangeAmountIsRejected() {
		assertThatThrownBy(() -> PosSession.open(PosSessionId.of(UUID.randomUUID()), UUID.randomUUID(),
				UUID.randomUUID(), CompanyId.of(UUID.randomUUID()), new BigDecimal("-1"), Instant.now()))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Rejects opening without an opening change amount")
	void openingWithoutAnOpeningChangeAmountIsRejected() {
		assertThatThrownBy(() -> PosSession.open(PosSessionId.of(UUID.randomUUID()), UUID.randomUUID(),
				UUID.randomUUID(), CompanyId.of(UUID.randomUUID()), null, Instant.now()))
				.isInstanceOf(BusinessRuleException.class);
	}
}
