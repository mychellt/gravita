package br.gravita.core.domain.masterdata;

import lombok.Getter;

import java.util.UUID;

/**
 * Numbering series for a fiscal document type, owned by a {@link Company}.
 * Created empty at company registration; configured later via
 * {@code ConfigureDocumentSeriesUseCase} (UC-04).
 */
@Getter
public final class DocumentSeries {

	private final UUID id;
	private final CompanyId companyId;
	private final FiscalDocumentType documentType;
	private final String series;
	private final Long nextNumber;

	private DocumentSeries(UUID id, CompanyId companyId, FiscalDocumentType documentType, String series, Long nextNumber) {
		this.id = id;
		this.companyId = companyId;
		this.documentType = documentType;
		this.series = series;
		this.nextNumber = nextNumber;
	}

	public static DocumentSeries of(UUID id, CompanyId companyId, FiscalDocumentType documentType, String series, Long nextNumber) {
		return new DocumentSeries(id, companyId, documentType, series, nextNumber);
	}

	public static DocumentSeries placeholder(CompanyId companyId, FiscalDocumentType documentType) {
		return new DocumentSeries(UUID.randomUUID(), companyId, documentType, null, 1L);
	}
}
