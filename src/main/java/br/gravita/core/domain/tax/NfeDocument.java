package br.gravita.core.domain.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.masterdata.CompanyId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.Getter;

/**
 * Outbound (B2B) NFe aggregate (UC-M2-01/03/04 - docs/specs/m2-fiscal-nfe.md
 * domain model). Built in {@link NfeDocumentStatus#DRAFT} by
 * {@link #draft}, then moved to {@link NfeDocumentStatus#QUEUED} by
 * {@link #queue} once a document number/access key has been allocated;
 * later states ({@code SENT}/{@code AUTHORIZED}/...) belong to UC-M2-03/04.
 * A referenced access key is required whenever {@code naturezaOperacao}
 * denotes a return or complementary note (AC6) - classified here by keyword
 * since the field is free text (module spec §3.1), matching how CFOP
 * registries are also keyed by free-text operation type.
 */
@Getter
public final class NfeDocument {

	private static final Pattern ACCESS_KEY_PATTERN = Pattern.compile("\\d{44}");

	private final NfeDocumentId id;
	private final CompanyId issuerCompanyId;
	private final UUID originSalesOrderId;
	private final String naturezaOperacao;
	private final NfeRecipient recipient;
	private final List<NfeItem> items;
	private final BigDecimal freight;
	private final BigDecimal insurance;
	private final BigDecimal otherExpenses;
	private final NfeTransport transport;
	private final String referencedAccessKey;
	private final String additionalInfo;
	private final TaxCalculationTotals taxTotals;
	private final NfeDocumentStatus status;
	private final String series;
	private final Long number;
	private final String accessKey;
	private final String protocol;
	private final Instant createdAt;

	private NfeDocument(NfeDocumentId id, CompanyId issuerCompanyId, UUID originSalesOrderId, String naturezaOperacao,
			NfeRecipient recipient, List<NfeItem> items, BigDecimal freight, BigDecimal insurance,
			BigDecimal otherExpenses, NfeTransport transport, String referencedAccessKey, String additionalInfo,
			TaxCalculationTotals taxTotals, NfeDocumentStatus status, String series, Long number, String accessKey,
			String protocol, Instant createdAt) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.issuerCompanyId = Objects.requireNonNull(issuerCompanyId, "issuerCompanyId is required");
		this.originSalesOrderId = originSalesOrderId;
		this.naturezaOperacao = requireText(naturezaOperacao, "naturezaOperacao");
		this.recipient = Objects.requireNonNull(recipient, "recipient is required");
		this.items = requireNonEmptyItems(items);
		this.freight = requireNonNegative(freight, "freight");
		this.insurance = requireNonNegative(insurance, "insurance");
		this.otherExpenses = requireNonNegative(otherExpenses, "otherExpenses");
		this.transport = transport;
		this.referencedAccessKey = requireReferencedAccessKeyWhenNeeded(naturezaOperacao, referencedAccessKey);
		this.additionalInfo = additionalInfo;
		this.taxTotals = Objects.requireNonNull(taxTotals, "taxTotals is required");
		this.status = Objects.requireNonNull(status, "status is required");
		this.series = series;
		this.number = number;
		this.accessKey = accessKey;
		this.protocol = protocol;
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
	}

	/** UC-M2-01: builds a new NFe awaiting numbering, in {@code DRAFT}. */
	public static NfeDocument draft(NfeDocumentId id, CompanyId issuerCompanyId, UUID originSalesOrderId,
			String naturezaOperacao, NfeRecipient recipient, List<NfeItem> items, BigDecimal freight,
			BigDecimal insurance, BigDecimal otherExpenses, NfeTransport transport, String referencedAccessKey,
			String additionalInfo, TaxCalculationTotals taxTotals) {
		return new NfeDocument(id, issuerCompanyId, originSalesOrderId, naturezaOperacao, recipient, items, freight,
				insurance, otherExpenses, transport, referencedAccessKey, additionalInfo, taxTotals,
				NfeDocumentStatus.DRAFT, null, null, null, null, Instant.now());
	}

	/** Rehydrates an existing record from storage, at any status. */
	public static NfeDocument of(NfeDocumentId id, CompanyId issuerCompanyId, UUID originSalesOrderId,
			String naturezaOperacao, NfeRecipient recipient, List<NfeItem> items, BigDecimal freight,
			BigDecimal insurance, BigDecimal otherExpenses, NfeTransport transport, String referencedAccessKey,
			String additionalInfo, TaxCalculationTotals taxTotals, NfeDocumentStatus status, String series,
			Long number, String accessKey, String protocol, Instant createdAt) {
		return new NfeDocument(id, issuerCompanyId, originSalesOrderId, naturezaOperacao, recipient, items, freight,
				insurance, otherExpenses, transport, referencedAccessKey, additionalInfo, taxTotals, status, series,
				number, accessKey, protocol, createdAt);
	}

	/**
	 * UC-M2-01: numbering has been allocated and the document handed to the
	 * transmission queue - only a {@code DRAFT} document can be queued, so a
	 * document is never numbered twice.
	 */
	public NfeDocument queue(String series, Long number, String accessKey) {
		if (status != NfeDocumentStatus.DRAFT) {
			throw new BusinessRuleException("NfeDocument " + id.value() + " is not DRAFT (current status: " + status + ")");
		}
		return new NfeDocument(id, issuerCompanyId, originSalesOrderId, naturezaOperacao, recipient, items, freight,
				insurance, otherExpenses, transport, referencedAccessKey, additionalInfo, taxTotals,
				NfeDocumentStatus.QUEUED, requireText(series, "series"), Objects.requireNonNull(number, "number is required"),
				requireAccessKey(accessKey), protocol, createdAt);
	}

	private static String requireReferencedAccessKeyWhenNeeded(String naturezaOperacao, String referencedAccessKey) {
		if (isReturnOrComplementary(naturezaOperacao) && (referencedAccessKey == null || referencedAccessKey.isBlank())) {
			throw new BusinessRuleException(
					"referencedAccessKey is required when naturezaOperacao is a return or complementary note: "
							+ naturezaOperacao);
		}
		return referencedAccessKey;
	}

	private static boolean isReturnOrComplementary(String naturezaOperacao) {
		String normalized = naturezaOperacao == null ? "" : naturezaOperacao.toUpperCase();
		return normalized.contains("DEVOLU") || normalized.contains("COMPLEMENTAR");
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

	private static BigDecimal requireNonNegative(BigDecimal value, String field) {
		BigDecimal resolved = value == null ? BigDecimal.ZERO : value;
		if (resolved.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException(field + " cannot be negative: " + resolved);
		}
		return resolved;
	}

	private static List<NfeItem> requireNonEmptyItems(List<NfeItem> items) {
		List<NfeItem> copy = items == null ? List.of() : List.copyOf(items);
		if (copy.isEmpty()) {
			throw new BusinessRuleException("An NFe must have at least one item");
		}
		return copy;
	}
}
