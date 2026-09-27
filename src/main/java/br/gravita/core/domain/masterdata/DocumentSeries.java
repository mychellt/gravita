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

	private DocumentSeries(UUID id, CompanyId companyId, FiscalDocumentType documentType, String series, Long nextNumber, Long version) {
		this.id = id;
		this.companyId = companyId;
		this.documentType = documentType;
		this.series = series;
		this.nextNumber = nextNumber;
		this.version = version;
	}

	public static DocumentSeries of(UUID id, CompanyId companyId, FiscalDocumentType documentType, String series, Long nextNumber, Long version) {
		return new DocumentSeries(id, companyId, documentType, series, nextNumber, version);
	}

	public static DocumentSeries placeholder(CompanyId companyId, FiscalDocumentType documentType) {
		return new DocumentSeries(UUID.randomUUID(), companyId, documentType, null, 1L, null);
	}

	public DocumentSeries reconfigure(String newSeries, Long newNextNumber) {
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
