package br.gravita.core.domain.masterdata;

import br.gravita.core.domain.shared.BusinessRuleException;
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

	/**
	 * UC-04: {@code series} is still {@code null} until the first configuration,
	 * so that call may set any starting {@code nextNumber}. Once configured, the
	 * series is considered live (UC-15 may already have allocated numbers from
	 * it), so {@code nextNumber} can only move forward, never backward, to avoid
	 * duplicate document numbers.
	 */
	public DocumentSeries reconfigure(String newSeries, Long newNextNumber) {
		if (this.series != null && newNextNumber < this.nextNumber) {
			throw new BusinessRuleException("nextNumber cannot be decreased once the series is already configured "
					+ "(current=" + this.nextNumber + ", requested=" + newNextNumber + ")");
		}
		return new DocumentSeries(this.id, this.companyId, this.documentType, newSeries, newNextNumber);
	}
}
