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
		return new DocumentSeries(this.id, this.companyId, this.documentType, newSeries, newNextNumber, this.version);
	}

	/**
	 * UC-15: reserves the current {@code nextNumber} and advances the counter by
	 * one. The caller must persist the result via optimistic locking on
	 * {@code version} - a losing concurrent write must retry from a fresh read
	 * rather than silently reuse an already-allocated number.
	 */
	public DocumentSeries allocateNext() {
		if (this.series == null) {
			throw new BusinessRuleException("Document series not configured for company " + this.companyId.value()
					+ " and type " + this.documentType);
		}
		return new DocumentSeries(this.id, this.companyId, this.documentType, this.series, this.nextNumber + 1, this.version);
	}
}
