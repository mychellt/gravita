package br.gravita.core.domain.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.masterdata.CompanyId;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
public final class PosSession {

	private final PosSessionId id;
	private final UUID registerId;
	private final UUID operatorId;
	private final CompanyId companyId;
	private final BigDecimal openingChangeAmount;
	private final PosSessionStatus status;
	private final Instant openedAt;
	private final Instant closedAt;

	private PosSession(PosSessionId id, UUID registerId, UUID operatorId, CompanyId companyId,
			BigDecimal openingChangeAmount, PosSessionStatus status, Instant openedAt, Instant closedAt) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.registerId = Objects.requireNonNull(registerId, "registerId is required");
		this.operatorId = Objects.requireNonNull(operatorId, "operatorId is required");
		this.companyId = Objects.requireNonNull(companyId, "companyId is required");
		this.openingChangeAmount = requireNonNegative(openingChangeAmount);
		this.status = Objects.requireNonNull(status, "status is required");
		this.openedAt = Objects.requireNonNull(openedAt, "openedAt is required");
		this.closedAt = closedAt;
	}

	public static PosSession open(PosSessionId id, UUID registerId, UUID operatorId, CompanyId companyId,
			BigDecimal openingChangeAmount, Instant openedAt) {
		return new PosSession(id, registerId, operatorId, companyId, openingChangeAmount, PosSessionStatus.OPEN,
				openedAt, null);
	}

	public static PosSession of(PosSessionId id, UUID registerId, UUID operatorId, CompanyId companyId,
			BigDecimal openingChangeAmount, PosSessionStatus status, Instant openedAt, Instant closedAt) {
		return new PosSession(id, registerId, operatorId, companyId, openingChangeAmount, status, openedAt, closedAt);
	}

	public PosSession close(Instant closedAt) {
		if (status != PosSessionStatus.OPEN) {
			throw new BusinessRuleException("PosSession " + id.value() + " is not open (current status: " + status + ")");
		}
		return new PosSession(id, registerId, operatorId, companyId, openingChangeAmount, PosSessionStatus.CLOSED,
				openedAt, Objects.requireNonNull(closedAt, "closedAt is required"));
	}

	private static BigDecimal requireNonNegative(BigDecimal openingChangeAmount) {
		if (openingChangeAmount == null || openingChangeAmount.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("openingChangeAmount must not be negative");
		}
		return openingChangeAmount;
	}
}
