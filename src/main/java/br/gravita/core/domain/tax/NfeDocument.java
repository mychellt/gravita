package br.gravita.core.domain.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.masterdata.CompanyId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
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

	/** UC-M2-05 (§3.2): "limit of 20 events." */
	public static final int MAX_CORRECTION_LETTERS = 20;

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
	private final boolean contingencyMode;
	private final String rejectionReason;
	private final String xmlStorageRef;
	private final String danfeStorageRef;
	private final List<CorrectionLetter> correctionLetters;

	private NfeDocument(NfeDocumentId id, CompanyId issuerCompanyId, UUID originSalesOrderId,
			NaturezaOperacao naturezaOperacao, Cfop cfop, NfeRecipient recipient, List<NfeItem> items,
			BigDecimal freight, BigDecimal insurance, BigDecimal otherExpenses, NfeTransportInfo transport,
			String referencedAccessKey, String additionalInfo, TaxCalculationTotals taxTotals,
			NfeDocumentStatus status, Instant createdAt, String documentSeries, Long documentNumber, String accessKey,
			String sefazProtocol, boolean contingencyMode, String rejectionReason, String xmlStorageRef,
			String danfeStorageRef, List<CorrectionLetter> correctionLetters) {
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
		this.contingencyMode = contingencyMode;
		this.rejectionReason = rejectionReason;
		this.xmlStorageRef = xmlStorageRef;
		this.danfeStorageRef = danfeStorageRef;
		this.correctionLetters = correctionLetters == null ? List.of() : List.copyOf(correctionLetters);
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
				NfeDocumentStatus.DRAFT, createdAt, null, null, null, null, false, null, null, null, List.of());
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
			String sefazProtocol, boolean contingencyMode, String rejectionReason, String xmlStorageRef,
			String danfeStorageRef, List<CorrectionLetter> correctionLetters) {
		return new NfeDocument(id, issuerCompanyId, originSalesOrderId, naturezaOperacao, cfop, recipient, items,
				freight, insurance, otherExpenses, transport, referencedAccessKey, additionalInfo, taxTotals, status,
				createdAt, documentSeries, documentNumber, accessKey, sefazProtocol, contingencyMode, rejectionReason,
				xmlStorageRef, danfeStorageRef, correctionLetters);
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
				Objects.requireNonNull(accessKey, "accessKey is required"), sefazProtocol, false, null, null, null,
				correctionLetters);
	}

	/**
	 * UC-M2-03 (AC1/AC2): marks the document as transmitted to SEFAZ - signing
	 * happens automatically inside {@code SubmitToSefazPort}'s adapter, not
	 * here. Only a {@code QUEUED} document can be sent; a document already
	 * {@code SENT} (a retry after a timeout) is re-submitted without
	 * transitioning again, so {@code TransmitNfeUseCase} calls this only once
	 * per document.
	 */
	public NfeDocument send() {
		requireQueued();
		return new NfeDocument(id, issuerCompanyId, originSalesOrderId, naturezaOperacao, cfop, recipient, items,
				freight, insurance, otherExpenses, transport, referencedAccessKey, additionalInfo, taxTotals,
				NfeDocumentStatus.SENT, createdAt, documentSeries, documentNumber, accessKey, sefazProtocol,
				contingencyMode, rejectionReason, xmlStorageRef, danfeStorageRef, correctionLetters);
	}

	/**
	 * UC-M2-03 (AC3): flips into SVC-AN/SVC-RS contingency after SEFAZ-UF has
	 * repeatedly timed out (doc §13). The access key itself is unchanged - per
	 * {@link NfeAccessKeyGenerator}'s note, contingency is a transmission-time
	 * routing decision, not a re-numbering event.
	 */
	public NfeDocument switchToContingency() {
		requireSent();
		return new NfeDocument(id, issuerCompanyId, originSalesOrderId, naturezaOperacao, cfop, recipient, items,
				freight, insurance, otherExpenses, transport, referencedAccessKey, additionalInfo, taxTotals, status,
				createdAt, documentSeries, documentNumber, accessKey, sefazProtocol, true, rejectionReason,
				xmlStorageRef, danfeStorageRef, correctionLetters);
	}

	/**
	 * UC-M2-03 (AC1/AC4/AC6): SEFAZ authorized the document. Carries the
	 * storage references for the XML/DANFE that were rendered and persisted
	 * as part of the same transmission attempt.
	 */
	public NfeDocument authorize(String sefazProtocol) {
		requireSent();
		return new NfeDocument(id, issuerCompanyId, originSalesOrderId, naturezaOperacao, cfop, recipient, items,
				freight, insurance, otherExpenses, transport, referencedAccessKey, additionalInfo, taxTotals,
				NfeDocumentStatus.AUTHORIZED, createdAt, documentSeries, documentNumber, accessKey,
				requireText(sefazProtocol, "sefazProtocol"), contingencyMode, null, xmlStorageRef, danfeStorageRef,
				correctionLetters);
	}

	/**
	 * UC-M2-03 (AC6): attaches the storage references for the XML/DANFE
	 * rendered from this now-{@code AUTHORIZED} document - a separate step
	 * from {@link #authorize}, since the XML/DANFE can only be rendered (and
	 * therefore stored) once the protocol they embed is known.
	 */
	public NfeDocument withStorageRefs(String xmlStorageRef, String danfeStorageRef) {
		if (status != NfeDocumentStatus.AUTHORIZED) {
			throw new BusinessRuleException(
					"NfeDocument " + id.value() + " is not AUTHORIZED (current status: " + status + ")");
		}
		return new NfeDocument(id, issuerCompanyId, originSalesOrderId, naturezaOperacao, cfop, recipient, items,
				freight, insurance, otherExpenses, transport, referencedAccessKey, additionalInfo, taxTotals, status,
				createdAt, documentSeries, documentNumber, accessKey, sefazProtocol, contingencyMode, rejectionReason,
				requireText(xmlStorageRef, "xmlStorageRef"), requireText(danfeStorageRef, "danfeStorageRef"),
				correctionLetters);
	}

	/**
	 * UC-M2-03: SEFAZ rejected the document outright (not a timeout) - the
	 * reason is surfaced so the issuer can correct and re-issue under a new
	 * document number, per the use case description.
	 */
	public NfeDocument reject(String rejectionReason) {
		requireSent();
		return new NfeDocument(id, issuerCompanyId, originSalesOrderId, naturezaOperacao, cfop, recipient, items,
				freight, insurance, otherExpenses, transport, referencedAccessKey, additionalInfo, taxTotals,
				NfeDocumentStatus.REJECTED, createdAt, documentSeries, documentNumber, accessKey, sefazProtocol,
				contingencyMode, requireText(rejectionReason, "rejectionReason"), xmlStorageRef, danfeStorageRef,
				correctionLetters);
	}

	/**
	 * UC-M2-05: registers a CC-e event correcting non-tax data on this
	 * {@code AUTHORIZED} document. AC: rejects documents that aren't
	 * {@code AUTHORIZED} and once {@link #MAX_CORRECTION_LETTERS} events
	 * already exist; the new event's sequence number is derived from how many
	 * already exist, and its SEFAZ protocol is supplied by the caller (the
	 * use case submits to SEFAZ before calling this method).
	 */
	public NfeDocument issueCorrectionLetter(String text, String protocol, Instant issuedAt) {
		requireAuthorized();
		if (correctionLetters.size() >= MAX_CORRECTION_LETTERS) {
			throw new BusinessRuleException("NfeDocument " + id.value() + " already has the maximum of "
					+ MAX_CORRECTION_LETTERS + " correction letters");
		}
		CorrectionLetter letter = new CorrectionLetter(correctionLetters.size() + 1, text, protocol, issuedAt);
		List<CorrectionLetter> updated = new ArrayList<>(correctionLetters);
		updated.add(letter);
		return new NfeDocument(id, issuerCompanyId, originSalesOrderId, naturezaOperacao, cfop, recipient, items,
				freight, insurance, otherExpenses, transport, referencedAccessKey, additionalInfo, taxTotals, status,
				createdAt, documentSeries, documentNumber, accessKey, sefazProtocol, contingencyMode, rejectionReason,
				xmlStorageRef, danfeStorageRef, updated);
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

	private void requireQueued() {
		if (status != NfeDocumentStatus.QUEUED) {
			throw new BusinessRuleException("NfeDocument " + id.value() + " is not QUEUED (current status: " + status + ")");
		}
	}

	private void requireSent() {
		if (status != NfeDocumentStatus.SENT) {
			throw new BusinessRuleException("NfeDocument " + id.value() + " is not SENT (current status: " + status + ")");
		}
	}

	private void requireAuthorized() {
		if (status != NfeDocumentStatus.AUTHORIZED) {
			throw new BusinessRuleException(
					"NfeDocument " + id.value() + " is not AUTHORIZED (current status: " + status + ")");
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
