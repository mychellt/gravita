package br.gravita.core.domain.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;

@Getter
public final class VoidedNumberRange {

	private final VoidedNumberRangeId id;
	private final CompanyId companyId;
	private final FiscalDocumentType documentType;
	private final String series;
	private final Long startNumber;
	private final Long endNumber;
	private final String justification;
	private final String sefazProtocol;
	private final Instant voidedAt;

	public VoidedNumberRange(final VoidedNumberRangeId id, final CompanyId companyId, final FiscalDocumentType documentType,
			final String series, final Long startNumber, final Long endNumber, final String justification, final String sefazProtocol,
			final Instant voidedAt) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.companyId = Objects.requireNonNull(companyId, "companyId is required");
		this.documentType = Objects.requireNonNull(documentType, "documentType is required");
		this.series = requireNonBlank(series, "series");
		this.startNumber = requirePositive(startNumber, "startNumber");
		this.endNumber = requirePositive(endNumber, "endNumber");
		if (this.endNumber < this.startNumber) {
			throw new BusinessRuleException("endNumber must not be less than startNumber");
		}
		this.justification = requireNonBlank(justification, "justification");
		this.sefazProtocol = requireNonBlank(sefazProtocol, "sefazProtocol");
		this.voidedAt = Objects.requireNonNull(voidedAt, "voidedAt is required");
	}

	public static VoidedNumberRange of(final VoidedNumberRangeId id, final CompanyId companyId, final FiscalDocumentType documentType,
			final String series, final Long startNumber, final Long endNumber, final String justification, final String sefazProtocol,
			final Instant voidedAt) {
		return new VoidedNumberRange(id, companyId, documentType, series, startNumber, endNumber, justification,
				sefazProtocol, voidedAt);
	}

	private static String requireNonBlank(final String value, final String field) {
		if (value == null || value.isBlank()) {
			throw new BusinessRuleException(field + " is required");
		}
		return value;
	}

	private static Long requirePositive(final Long value, final String field) {
		if (value == null || value <= 0) {
			throw new BusinessRuleException(field + " must be positive");
		}
		return value;
	}
}
