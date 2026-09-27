package br.gravita.core.domain.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.masterdata.CompanyId;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

/**
 * An `Inutilização` record (UC-M2-06, doc §3.2): the formal void of a range
 * of document numbers that were allocated but never used. Immutable once
 * created - there is deliberately no update or delete operation, since the
 * numbering gap it explains must remain traceable for audit and SPED/Livros
 * Fiscais purposes (UC-M2-13).
 */
@Getter
public final class VoidedNumberRange {

	private final VoidedNumberRangeId id;
	private final CompanyId companyId;
	private final String series;
	private final Long startNumber;
	private final Long endNumber;
	private final String justification;
	private final String protocol;
	private final Instant voidedAt;

	private VoidedNumberRange(VoidedNumberRangeId id, CompanyId companyId, String series, Long startNumber,
			Long endNumber, String justification, String protocol, Instant voidedAt) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.companyId = Objects.requireNonNull(companyId, "companyId is required");
		this.series = requireNonBlank(series, "series is required");
		this.startNumber = Objects.requireNonNull(startNumber, "startNumber is required");
		this.endNumber = Objects.requireNonNull(endNumber, "endNumber is required");
		if (startNumber > endNumber) {
			throw new BusinessRuleException(
					"startNumber (" + startNumber + ") cannot be greater than endNumber (" + endNumber + ")");
		}
		this.justification = requireNonBlank(justification, "justification is required");
		this.protocol = Objects.requireNonNull(protocol, "protocol is required");
		this.voidedAt = Objects.requireNonNull(voidedAt, "voidedAt is required");
	}

	public static VoidedNumberRange of(VoidedNumberRangeId id, CompanyId companyId, String series, Long startNumber,
			Long endNumber, String justification, String protocol, Instant voidedAt) {
		return new VoidedNumberRange(id, companyId, series, startNumber, endNumber, justification, protocol, voidedAt);
	}

	private static String requireNonBlank(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new BusinessRuleException(message);
		}
		return value;
	}
}
