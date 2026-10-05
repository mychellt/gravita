package br.gravita.core.domain.tax;

import lombok.Builder;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.Getter;

@Getter
public final class InboundNfe {

	private static final Pattern ACCESS_KEY_PATTERN = Pattern.compile("\\d{44}");
	private static final String MANUAL_ENTRY_XML_STORAGE_REF = "MANUAL_ENTRY";

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
	private final List<InboundNfeConferenceItem> conferenceResult;

	@Builder
	public InboundNfe(final InboundNfeId id, final CompanyId companyId, final String accessKey, final String series, final String number,
			final Document supplierDocument, final String supplierName, final Instant issuedAt, final List<InboundNfeItem> items,
			final InboundNfeTotals totals, final String xmlStorageRef, final InboundNfeStatus status, final Instant importedAt,
			final List<InboundNfeConferenceItem> conferenceResult) {
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
		this.conferenceResult = conferenceResult == null ? List.of() : List.copyOf(conferenceResult);
	}

	@Builder(builderMethodName = "importedFromXml", builderClassName = "ImportedFromXmlBuilder")
	private InboundNfe(final InboundNfeId id, final CompanyId companyId, final String accessKey, final String series,
			final String number, final Document supplierDocument, final String supplierName, final Instant issuedAt,
			final List<InboundNfeItem> items, final InboundNfeTotals totals, final String xmlStorageRef) {
		this(id, companyId, accessKey, series, number, supplierDocument, supplierName, issuedAt, items, totals,
				xmlStorageRef, InboundNfeStatus.PENDING_CONFERENCE, Instant.now(), List.of());
	}

	public static InboundNfe enteredManually(final InboundNfeId id, final CompanyId companyId, final String accessKey, final String series,
			final String number, final Document supplierDocument, final String supplierName, final Instant issuedAt,
			final List<InboundNfeItem> items, final InboundNfeTotals totals) {
		return new InboundNfe(id, companyId, accessKey, series, number, supplierDocument, supplierName, issuedAt,
				items, totals, MANUAL_ENTRY_XML_STORAGE_REF, InboundNfeStatus.PENDING_CONFERENCE, Instant.now(),
				List.of());
	}

	public InboundNfe confirm(final List<InboundNfeConferenceItem> conferenceResult) {
		if (status == InboundNfeStatus.CONFIRMED) {
			throw new BusinessRuleException("Inbound NFe already confirmed: " + id.value());
		}
		final List<InboundNfeConferenceItem> result = requireConferenceResult(conferenceResult);
		return new InboundNfe(id, companyId, accessKey, series, number, supplierDocument, supplierName, issuedAt,
				items, totals, xmlStorageRef, InboundNfeStatus.CONFIRMED, importedAt, result);
	}

	private List<InboundNfeConferenceItem> requireConferenceResult(final List<InboundNfeConferenceItem> conferenceResult) {
		final List<InboundNfeConferenceItem> copy = conferenceResult == null ? List.of() : List.copyOf(conferenceResult);
		if (copy.size() != items.size()) {
			throw new BusinessRuleException("Conference result must have exactly one entry per NFe item (expected "
					+ items.size() + ", got " + copy.size() + ")");
		}
		return copy;
	}

	private static String requireAccessKey(final String accessKey) {
		if (accessKey == null || !ACCESS_KEY_PATTERN.matcher(accessKey).matches()) {
			throw new BusinessRuleException("NFe access key must be 44 digits: " + accessKey);
		}
		return accessKey;
	}

	private static String requireText(final String value, final String field) {
		if (value == null || value.isBlank()) {
			throw new BusinessRuleException(field + " is required");
		}
		return value;
	}

	private static List<InboundNfeItem> requireNonEmptyItems(final List<InboundNfeItem> items) {
		final List<InboundNfeItem> copy = items == null ? List.of() : List.copyOf(items);
		if (copy.isEmpty()) {
			throw new BusinessRuleException("An inbound NFe must have at least one item");
		}
		return copy;
	}
}
