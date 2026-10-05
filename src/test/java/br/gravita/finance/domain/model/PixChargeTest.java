package br.gravita.finance.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.finance.PixCharge;
import br.gravita.core.domain.finance.PixChargeId;
import br.gravita.core.domain.finance.PixChargeStatus;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.finance.PixPayloads;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PixChargeTest {

	private static final Instant NOW = Instant.parse("2026-09-28T12:00:00Z");

	private final Receivable receivable = Receivable.createManual(ReceivableId.of(UUID.randomUUID()),
			UUID.randomUUID(), new BigDecimal("150.00"), LocalDate.of(2026, 10, 30), null);

	private PixCharge issue(final String payload, final Instant expiresAt) {
		return PixCharge.issue(PixChargeId.of(UUID.randomUUID()), receivable, payload, expiresAt, NOW);
	}

	private PixCharge pending() {
		return issue(PixPayloads.valid(), NOW.plus(1, ChronoUnit.DAYS));
	}

	@Test
	@DisplayName("Starts an issued charge as pending, carrying the receivable's amount and due date")
	void anIssuedChargeCarriesTheReceivablesAmountAndDueDateAndStartsPending() {
		final PixCharge charge = pending();

		assertThat(charge.getReceivableId()).isEqualTo(receivable.getId());
		assertThat(charge.getAmount()).isEqualByComparingTo("150.00");
		assertThat(charge.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 30));
		assertThat(charge.getDynamicQrPayload()).isEqualTo(PixPayloads.valid());
		assertThat(charge.getStatus()).isEqualTo(PixChargeStatus.PENDING);
	}

	@Test
	@DisplayName("Rejects a charge with a missing payload")
	void rejectsAMissingPayload() {
		assertThatThrownBy(() -> issue(null, NOW.plusSeconds(60))).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> issue(" ", NOW.plusSeconds(60))).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Rejects a payload that is not an EMV BR Code")
	void rejectsAPayloadThatIsNotAnEmvBrCode() {
		assertThatThrownBy(() -> issue("not-a-pix-payload", NOW.plusSeconds(60)))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("000201");
	}

	@Test
	@DisplayName("Rejects a payload without the CRC field")
	void rejectsAPayloadWithoutTheCrcField() {
		assertThatThrownBy(() -> issue("00020126360014br.gov.bcb.pix5303986", NOW.plusSeconds(60)))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("CRC16");
	}

	@Test
	@DisplayName("Rejects a payload with an invalid CRC")
	void rejectsAPayloadWithABadCrc() {
		final String valid = PixPayloads.valid();
		final String corrupted = valid.substring(0, valid.length() - 4) + "0000";

		assertThatThrownBy(() -> issue(corrupted, NOW.plusSeconds(60))).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("invalid CRC16");
	}

	@Test
	@DisplayName("Rejects a charge whose expiry is already in the past")
	void rejectsAChargeThatAlreadyExpires() {
		assertThatThrownBy(() -> issue(PixPayloads.valid(), NOW)).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("future");
	}

	@Test
	@DisplayName("Expires the charge only once its expiry time has passed")
	void expiresOnlyOncePastExpiresAt() {
		final PixCharge charge = pending();

		assertThat(charge.expireIfDue(charge.getExpiresAt()).getStatus()).isEqualTo(PixChargeStatus.PENDING);
		assertThat(charge.expireIfDue(charge.getExpiresAt().plusSeconds(1)).getStatus())
				.isEqualTo(PixChargeStatus.EXPIRED);
	}

	@Test
	@DisplayName("Never expires a charge that is already paid")
	void paidChargeNeverExpires() {
		final PixCharge paid = pending().markPaid();

		assertThat(paid.isDueForExpiry(paid.getExpiresAt().plusSeconds(1))).isFalse();
		assertThat(paid.expireIfDue(paid.getExpiresAt().plusSeconds(1)).getStatus())
				.isEqualTo(PixChargeStatus.PAID);
	}

	@Test
	@DisplayName("Moves the charge to paid even when it has already expired")
	void payingMovesToPaidEvenWhenAlreadyExpired() {
		final PixCharge expired = pending().expireIfDue(NOW.plus(2, ChronoUnit.DAYS));

		assertThat(expired.getStatus()).isEqualTo(PixChargeStatus.EXPIRED);
		assertThat(expired.markPaid().getStatus()).isEqualTo(PixChargeStatus.PAID);
	}

	@Test
	@DisplayName("Rejects paying a charge that is already paid")
	void payingAnAlreadyPaidChargeIsRejected() {
		assertThatThrownBy(() -> pending().markPaid().markPaid()).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("already PAID");
	}
}
