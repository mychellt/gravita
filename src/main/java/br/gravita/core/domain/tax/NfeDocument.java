package br.gravita.core.domain.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.masterdata.CompanyId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * NfeDocument aggregate (UC-M2-01). Follows {@link NfceSale}'s shape - a
 * private constructor plus static factories, and state-transition methods
 * that return a new immutable instance rather than mutating {@code this} -
 * but its own fields, since a B2B NFe carries a recipient, transport and
 * referenced-document data that a walk-in NFC-e sale never needs.
 */
@Getter
public final class NfeDocument {

	private final NfeDocumentId id;
	private final CompanyId issuerCompanyId;
	private final UUID originSalesOrderId;
	private final NaturezaOperacao naturezaOperacao;
	private final Cfop cfop;
	private final NfeRecipient recipient;
	private final List<NfeItem> items;
	private final BigDecimal freight;
	private final BigDecimal insurance;
	private final BigDecimal otherExpenses;
	private final NfeTransportInfo transport;
	private final String referencedAccessKey;
	private final String additionalInfo;
	private final TaxCalculationTotals taxTotals;
	private final NfeDocumentStatus status;
	private final Instant createdAt;
	private final String documentSeries;
	private final Long documentNumber;
	private final String accessKey;
	private final String sefazProtocol;

	private NfeDocument(NfeDocumentId id, CompanyId issuerCompanyId, UUID originSalesOrderId,
			NaturezaOperacao naturezaOperacao, Cfop cfop, NfeRecipient recipient, List<NfeItem> items,
			BigDecimal freight, BigDecimal insurance, BigDecimal otherExpenses, NfeTransportInfo transport,
			String referencedAccessKey, String additionalInfo, TaxCalculationTotals taxTotals,
			NfeDocumentStatus status, Instant createdAt, String documentSeries, Long documentNumber, String accessKey,
			String sefazProtocol) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.issuerCompanyId = Objects.requireNonNull(issuerCompanyId, "issuerCompanyId is required");
		this.originSalesOrderId = originSalesOrderId;
		this.naturezaOperacao = Objects.requireNonNull(naturezaOperacao, "naturezaOperacao is required");
		this.cfop = Objects.requireNonNull(cfop, "cfop is required");
		this.recipient = Objects.requireNonNull(recipient, "recipient is required");
		this.items = requireNonEmptyItems(items);
		this.freight = requireNonNegative(freight, "freight");
		this.insurance = requireNonNegative(insurance, "insurance");
		this.otherExpenses = requireNonNegative(otherExpenses, "otherExpenses");
		this.transport = transport;
		this.referencedAccessKey = requireReferencedAccessKeyIfNeeded(naturezaOperacao, referencedAccessKey);
		this.additionalInfo = additionalInfo;
		this.taxTotals = Objects.requireNonNull(taxTotals, "taxTotals is required");
		this.status = Objects.requireNonNull(status, "status is required");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
		this.documentSeries = documentSeries;
		this.documentNumber = documentNumber;
		this.accessKey = accessKey;
		this.sefazProtocol = sefazProtocol;
	}

	/**
	 * Builds a new {@code DRAFT} document (UC-M2-01). AC6: a return or
	 * complementary note must carry a {@code referencedAccessKey}.
	 */
	public static NfeDocument draft(NfeDocumentId id, CompanyId issuerCompanyId, UUID originSalesOrderId,
			NaturezaOperacao naturezaOperacao, Cfop cfop, NfeRecipient recipient, List<NfeItem> items,
			BigDecimal freight, BigDecimal insurance, BigDecimal otherExpenses, NfeTransportInfo transport,
			String referencedAccessKey, String additionalInfo, TaxCalculationTotals taxTotals, Instant createdAt) {
		return new NfeDocument(id, issuerCompanyId, originSalesOrderId, naturezaOperacao, cfop, recipient, items,
				freight, insurance, otherExpenses, transport, referencedAccessKey, additionalInfo, taxTotals,
				NfeDocumentStatus.DRAFT, createdAt, null, null, null, null);
	}

	/**
	 * Reconstructs a document from persistence, at any status in its
	 * lifecycle.
	 */
	public static NfeDocument of(NfeDocumentId id, CompanyId issuerCompanyId, UUID originSalesOrderId,
			NaturezaOperacao naturezaOperacao, Cfop cfop, NfeRecipient recipient, List<NfeItem> items,
			BigDecimal freight, BigDecimal insurance, BigDecimal otherExpenses, NfeTransportInfo transport,
			String referencedAccessKey, String additionalInfo, TaxCalculationTotals taxTotals,
			NfeDocumentStatus status, Instant createdAt, String documentSeries, Long documentNumber, String accessKey,
			String sefazProtocol) {
		return new NfeDocument(id, issuerCompanyId, originSalesOrderId, naturezaOperacao, cfop, recipient, items,
				freight, insurance, otherExpenses, transport, referencedAccessKey, additionalInfo, taxTotals, status,
				createdAt, documentSeries, documentNumber, accessKey, sefazProtocol);
	}

	/**
	 * AC7: numbers the document and moves it to {@code QUEUED} for
	 * transmission. Only a {@code DRAFT} document can be queued - re-queuing
	 * an already-decided document would allocate a second document number for
	 * the same commercial transaction.
	 */
	public NfeDocument queue(String documentSeries, Long documentNumber, String accessKey) {
		requireDraft();
		return new NfeDocument(id, issuerCompanyId, originSalesOrderId, naturezaOperacao, cfop, recipient, items,
				freight, insurance, otherExpenses, transport, referencedAccessKey, additionalInfo, taxTotals,
				NfeDocumentStatus.QUEUED, createdAt, requireText(documentSeries, "documentSeries"),
				Objects.requireNonNull(documentNumber, "documentNumber is required"),
				Objects.requireNonNull(accessKey, "accessKey is required"), sefazProtocol);
	}

	public BigDecimal getItemsSubtotal() {
		return items.stream().map(NfeItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal getDocumentTotal() {
		return getItemsSubtotal().add(freight).add(insurance).add(otherExpenses);
	}

	private void requireDraft() {
		if (status != NfeDocumentStatus.DRAFT) {
			throw new BusinessRuleException("NfeDocument " + id.value() + " is not DRAFT (current status: " + status + ")");
		}
	}

	private static String requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new BusinessRuleException(field + " is required");
		}
		return value;
	}

	private static List<NfeItem> requireNonEmptyItems(List<NfeItem> items) {
		if (items == null || items.isEmpty()) {
			throw new BusinessRuleException("An NfeDocument must have at least one item");
		}
		return List.copyOf(items);
	}

	private static BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
		BigDecimal resolved = value == null ? BigDecimal.ZERO : value;
		if (resolved.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException(fieldName + " cannot be negative: " + resolved);
		}
		return resolved;
	}

	private static String requireReferencedAccessKeyIfNeeded(NaturezaOperacao naturezaOperacao,
			String referencedAccessKey) {
		if (naturezaOperacao.requiresReferencedAccessKey() && (referencedAccessKey == null
				|| referencedAccessKey.isBlank())) {
			throw new BusinessRuleException(
					"referencedAccessKey is required when naturezaOperacao is " + naturezaOperacao);
		}
		return referencedAccessKey;
	}
}
