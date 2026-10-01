package br.gravita.tax.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.tax.CashMovement;
import br.gravita.core.domain.tax.CashMovementId;
import br.gravita.core.domain.tax.CashMovementType;
import br.gravita.core.domain.tax.PosSessionId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CashMovementTest {

	@Test
	@DisplayName("Recording a sangria or suprimento captures the given type, amount and session")
	void ac1_recordingASangriaOrSuprimentoCapturesTheGivenTypeAmountAndSession() {
		PosSessionId sessionId = PosSessionId.of(UUID.randomUUID());
		Instant now = Instant.now();

		CashMovement sangria = CashMovement.of(CashMovementId.of(UUID.randomUUID()), sessionId,
				CashMovementType.SANGRIA, new BigDecimal("50.00"), "Deposit at bank", now);
		CashMovement suprimento = CashMovement.of(CashMovementId.of(UUID.randomUUID()), sessionId,
				CashMovementType.SUPRIMENTO, new BigDecimal("30.00"), "Change top-up", now);

		assertThat(sangria.getType()).isEqualTo(CashMovementType.SANGRIA);
		assertThat(suprimento.getType()).isEqualTo(CashMovementType.SUPRIMENTO);
		assertThat(sangria.getSessionId()).isEqualTo(sessionId);
		assertThat(sangria.getAmount()).isEqualByComparingTo("50.00");
	}

	@Test
	@DisplayName("Rejects a blank justification")
	void ac2_blankJustificationIsRejected() {
		assertThatThrownBy(() -> CashMovement.of(CashMovementId.of(UUID.randomUUID()),
				PosSessionId.of(UUID.randomUUID()), CashMovementType.SANGRIA, new BigDecimal("50.00"), " ",
				Instant.now()))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Rejects a null justification")
	void ac2_nullJustificationIsRejected() {
		assertThatThrownBy(() -> CashMovement.of(CashMovementId.of(UUID.randomUUID()),
				PosSessionId.of(UUID.randomUUID()), CashMovementType.SANGRIA, new BigDecimal("50.00"), null,
				Instant.now()))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Records the given timestamp")
	void ac3_theGivenTimestampIsRecorded() {
		Instant now = Instant.now();

		CashMovement movement = CashMovement.of(CashMovementId.of(UUID.randomUUID()),
				PosSessionId.of(UUID.randomUUID()), CashMovementType.SANGRIA, new BigDecimal("50.00"), "Justified",
				now);

		assertThat(movement.getTimestamp()).isEqualTo(now);
	}

	@Test
	@DisplayName("Rejects a non-positive amount")
	void aNonPositiveAmountIsRejected() {
		assertThatThrownBy(() -> CashMovement.of(CashMovementId.of(UUID.randomUUID()),
				PosSessionId.of(UUID.randomUUID()), CashMovementType.SANGRIA, BigDecimal.ZERO, "Justified",
				Instant.now()))
				.isInstanceOf(BusinessRuleException.class);
	}
}
