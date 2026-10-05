package br.gravita.core.domain.finance;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import lombok.Getter;

/**
 * A dynamic PIX QR code charging one {@link Receivable}. Immutable: every
 * transition returns a new instance.
 */
@Getter
public final class PixCharge {

	private static final String PAYLOAD_FORMAT_INDICATOR = "000201";
	private static final String CRC_FIELD_PREFIX = "6304";
	private static final int CRC_HEX_LENGTH = 4;

	private final PixChargeId id;
	private final ReceivableId receivableId;
	private final String dynamicQrPayload;
	private final BigDecimal amount;
	private final LocalDate dueDate;
	private final Instant expiresAt;
	private final PixChargeStatus status;

	public PixCharge(final PixChargeId id, final ReceivableId receivableId, final String dynamicQrPayload, final BigDecimal amount,
			final LocalDate dueDate, final Instant expiresAt, final PixChargeStatus status) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.receivableId = Objects.requireNonNull(receivableId, "receivableId is required");
		this.dynamicQrPayload = requireValidPayload(dynamicQrPayload);
		this.amount = requirePositive(amount);
		this.dueDate = Objects.requireNonNull(dueDate, "dueDate is required");
		this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt is required");
		this.status = Objects.requireNonNull(status, "status is required");
	}

	/** A charge for the receivable's amount and due date; starts {@code PENDING} and must expire after {@code now}. */
	public static PixCharge issue(final PixChargeId id, final Receivable receivable, final String dynamicQrPayload, final Instant expiresAt,
			final Instant now) {
		Objects.requireNonNull(receivable, "receivable is required");
		if (expiresAt != null && !expiresAt.isAfter(now)) {
			throw new BusinessRuleException("expiresAt must be in the future: " + expiresAt);
		}
		return new PixCharge(id, receivable.getId(), dynamicQrPayload, receivable.getAmount(),
				receivable.getDueDate(), expiresAt, PixChargeStatus.PENDING);
	}

	public static PixCharge of(final PixChargeId id, final ReceivableId receivableId, final String dynamicQrPayload, final BigDecimal amount,
			final LocalDate dueDate, final Instant expiresAt, final PixChargeStatus status) {
		return new PixCharge(id, receivableId, dynamicQrPayload, amount, dueDate, expiresAt, status);
	}

	/** Whether this charge is still {@code PENDING} although {@code expiresAt} has passed. */
	public boolean isDueForExpiry(final Instant now) {
		return status == PixChargeStatus.PENDING && now.isAfter(expiresAt);
	}

	/** {@code EXPIRED} if unpaid past {@code expiresAt}; otherwise this charge unchanged. */
	public PixCharge expireIfDue(final Instant now) {
		return isDueForExpiry(now) ? withStatus(PixChargeStatus.EXPIRED) : this;
	}

	/**
	 * The bank confirmed the payment. An already {@code EXPIRED} charge can still
	 * be paid - the bank is the authority on whether the money arrived - so only
	 * a charge that is already {@code PAID} is rejected.
	 */
	public PixCharge markPaid() {
		if (status == PixChargeStatus.PAID) {
			throw new BusinessRuleException("PixCharge " + id.value() + " is already PAID");
		}
		return withStatus(PixChargeStatus.PAID);
	}

	private PixCharge withStatus(final PixChargeStatus newStatus) {
		return new PixCharge(id, receivableId, dynamicQrPayload, amount, dueDate, expiresAt, newStatus);
	}

	private static BigDecimal requirePositive(final BigDecimal amount) {
		if (amount == null) {
			throw new BusinessRuleException("amount is required");
		}
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("amount must be positive: " + amount);
		}
		return amount;
	}

	/**
	 * A PIX "copia e cola" (EMV BR Code): starts with the payload format
	 * indicator {@code 000201} and ends with the CRC16 field {@code 6304XXXX},
	 * where XXXX is the CRC-16/CCITT-FALSE of everything before it.
	 */
	private static String requireValidPayload(final String payload) {
		if (payload == null || payload.isBlank()) {
			throw new BusinessRuleException("dynamicQrPayload is required");
		}
		if (!payload.startsWith(PAYLOAD_FORMAT_INDICATOR)) {
			throw new BusinessRuleException("dynamicQrPayload must start with " + PAYLOAD_FORMAT_INDICATOR);
		}
		final int crcFieldStart = payload.length() - CRC_HEX_LENGTH - CRC_FIELD_PREFIX.length();
		if (crcFieldStart <= PAYLOAD_FORMAT_INDICATOR.length()
				|| !payload.startsWith(CRC_FIELD_PREFIX, crcFieldStart)) {
			throw new BusinessRuleException("dynamicQrPayload must end with the CRC16 field " + CRC_FIELD_PREFIX + "XXXX");
		}
		final String expectedCrc = String.format("%04X", crc16(payload.substring(0, payload.length() - CRC_HEX_LENGTH)));
		if (!payload.endsWith(expectedCrc)) {
			throw new BusinessRuleException("dynamicQrPayload has an invalid CRC16");
		}
		return payload;
	}

	private static int crc16(final String content) {
		int crc = 0xFFFF;
		for (final byte b : content.getBytes(StandardCharsets.UTF_8)) {
			crc ^= (b & 0xFF) << 8;
			for (int bit = 0; bit < 8; bit++) {
				crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
			}
		}
		return crc & 0xFFFF;
	}
}
