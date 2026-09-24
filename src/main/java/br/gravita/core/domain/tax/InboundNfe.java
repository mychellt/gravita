package br.gravita.core.domain.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.Getter;

/**
 * A purchase-side NFe (UC-M2-08/09/10 - docs/specs/m2-fiscal-nfe.md domain
 * model): imported/entered from a supplier, referenced by {@code accessKey}
 * (UC-M2-07's manifestation works by access key alone) and left
 * {@link InboundNfeStatus#PENDING_CONFERENCE} until UC-M2-10 reconciles it
 * against what was ordered/received and confirms it into stock/payables.
 */
@Getter
public final class InboundNfe {

	private static final Pattern ACCESS_KEY_PATTERN = Pattern.compile("\\d{44}");

	private final InboundNfeId id;
	private final CompanyId companyId;
	private final String accessKey;
	private final String series;
	private final String number;
	private final Document supplierDocument;
	private final String supplierName;
	private final Instant issuedAt;
	private final List<InboundNfeItem> items;
	private final InboundNfeTotals totals;
	private final String xmlStorageRef;
	private final InboundNfeStatus status;
	private final Instant importedAt;

	private InboundNfe(InboundNfeId id, CompanyId companyId, String accessKey, String series, String number,
			Document supplierDocument, String supplierName, Instant issuedAt, List<InboundNfeItem> items,
			InboundNfeTotals totals, String xmlStorageRef, InboundNfeStatus status, Instant importedAt) {
		this.id = Objects.requireNonNull(id, "InboundNfeId is required");
		this.companyId = Objects.requireNonNull(companyId, "companyId is required");
		this.accessKey = requireAccessKey(accessKey);
		this.series = requireText(series, "series");
		this.number = requireText(number, "number");
		this.supplierDocument = Objects.requireNonNull(supplierDocument, "supplierDocument is required");
		this.supplierName = requireText(supplierName, "supplierName");
		this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt is required");
		this.items = requireNonEmptyItems(items);
		this.totals = Objects.requireNonNull(totals, "totals is required");
		this.xmlStorageRef = requireText(xmlStorageRef, "xmlStorageRef");
		this.status = Objects.requireNonNull(status, "status is required");
		this.importedAt = Objects.requireNonNull(importedAt, "importedAt is required");
	}

	/**
	 * UC-M2-08: creates an {@code InboundNfe} freshly parsed from a supplier's
	 * XML, ready for conference - never confirmed into stock/payables yet.
	 */
	public static InboundNfe importedFromXml(InboundNfeId id, CompanyId companyId, String accessKey, String series,
			String number, Document supplierDocument, String supplierName, Instant issuedAt,
			List<InboundNfeItem> items, InboundNfeTotals totals, String xmlStorageRef) {
		return new InboundNfe(id, companyId, accessKey, series, number, supplierDocument, supplierName, issuedAt,
				items, totals, xmlStorageRef, InboundNfeStatus.PENDING_CONFERENCE, Instant.now());
	}

	/** Rehydrates an existing record from storage. */
	public static InboundNfe of(InboundNfeId id, CompanyId companyId, String accessKey, String series, String number,
			Document supplierDocument, String supplierName, Instant issuedAt, List<InboundNfeItem> items,
			InboundNfeTotals totals, String xmlStorageRef, InboundNfeStatus status, Instant importedAt) {
		return new InboundNfe(id, companyId, accessKey, series, number, supplierDocument, supplierName, issuedAt,
				items, totals, xmlStorageRef, status, importedAt);
	}

	private static String requireAccessKey(String accessKey) {
		if (accessKey == null || !ACCESS_KEY_PATTERN.matcher(accessKey).matches()) {
			throw new BusinessRuleException("NFe access key must be 44 digits: " + accessKey);
		}
		return accessKey;
	}

	private static String requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new BusinessRuleException(field + " is required");
		}
		return value;
	}

	private static List<InboundNfeItem> requireNonEmptyItems(List<InboundNfeItem> items) {
		List<InboundNfeItem> copy = items == null ? List.of() : List.copyOf(items);
		if (copy.isEmpty()) {
			throw new BusinessRuleException("An inbound NFe must have at least one item");
		}
		return copy;
	}
}
