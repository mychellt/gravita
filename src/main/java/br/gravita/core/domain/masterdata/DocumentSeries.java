package br.gravita.core.domain.masterdata;

import br.gravita.core.domain.shared.BusinessRuleException;
import lombok.Getter;

import java.util.UUID;

@Getter
public final class DocumentSeries {

	private final UUID id;
	private final CompanyId companyId;
	private final FiscalDocumentType documentType;
	private final String series;
	private final Long nextNumber;
	private final Long version;

	public DocumentSeries(final UUID id, final CompanyId companyId, final FiscalDocumentType documentType, final String series,
			final Long nextNumber, final Long version) {
		this.id = id;
		this.companyId = companyId;
		this.documentType = documentType;
		this.series = series;
		this.nextNumber = nextNumber;
		this.version = version;
	}

	public static DocumentSeries of(final UUID id, final CompanyId companyId, final FiscalDocumentType documentType,
			final String series, final Long nextNumber, final Long version) {
		return new DocumentSeries(id, companyId, documentType, series, nextNumber, version);
	}

	public static DocumentSeries placeholder(final CompanyId companyId, final FiscalDocumentType documentType) {
		return new DocumentSeries(UUID.randomUUID(), companyId, documentType, null, 1L, null);
	}

	public DocumentSeries reconfigure(final String newSeries, final Long newNextNumber) {
		if (this.series != null && newNextNumber < this.nextNumber) {
			throw new BusinessRuleException("nextNumber cannot be decreased once the series is already configured "
					+ "(current=" + this.nextNumber + ", requested=" + newNextNumber + ")");
		}
		return new DocumentSeries(this.id, this.companyId, this.documentType, newSeries, newNextNumber, this.version);
	}

	public DocumentSeries allocateNext() {
		if (this.series == null) {
			throw new BusinessRuleException("Document series not configured for company " + this.companyId.value()
					+ " and type " + this.documentType);
		}
		return new DocumentSeries(this.id, this.companyId, this.documentType, this.series, this.nextNumber + 1, this.version);
	}
}
