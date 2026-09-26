package br.gravita.core.domain.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.Getter;

/**
 * NfceSale aggregate (UC-M3-03). Built from the cart ({@link SaleItem}s) plus
 * one or more {@link Payment}s; max-discount enforcement against the linked
 * price table (AC2) happens in the use case before {@link #register} is
 * called, since a single aggregate instance has no visibility into
 * masterdata's {@code PriceTable} — the same split used by {@link PosSession}
 * for its one-open-session-per-register check. {@code changeGiven} is always
 * derived here, never accepted as an input (AC4).
 */
@Getter
public final class NfceSale {

	private final NfceSaleId id;
	private final PosSessionId sessionId;
	private final List<SaleItem> items;
	private final BigDecimal totalDiscount;
	private final List<Payment> payments;
	private final BigDecimal changeGiven;
	private final String customerCpf;
	private final NfceSaleStatus status;
	private final Instant createdAt;
	private final String documentSeries;
	private final Long documentNumber;
	private final String accessKey;
	private final String sefazProtocol;
	private final boolean contingencyMode;

	private NfceSale(NfceSaleId id, PosSessionId sessionId, List<SaleItem> items, BigDecimal totalDiscount,
			List<Payment> payments, BigDecimal changeGiven, String customerCpf, NfceSaleStatus status,
			Instant createdAt, String documentSeries, Long documentNumber, String accessKey, String sefazProtocol,
			boolean contingencyMode) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.sessionId = Objects.requireNonNull(sessionId, "sessionId is required");
		this.items = requireNonEmptyItems(items);
		this.totalDiscount = requireNonNegative(totalDiscount, "totalDiscount");
		this.payments = requireNonEmptyPayments(payments);
		this.changeGiven = requireNonNegative(changeGiven, "changeGiven");
		this.customerCpf = normalizeCpf(customerCpf);
		this.status = Objects.requireNonNull(status, "status is required");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
		this.documentSeries = documentSeries;
		this.documentNumber = documentNumber;
		this.accessKey = accessKey;
		this.sefazProtocol = sefazProtocol;
		this.contingencyMode = contingencyMode;
	}

	/**
	 * Registers a new DRAFT sale (AC5): total payments must cover the sale
	 * total or the sale is rejected; {@code changeGiven} is computed as
	 * {@code sum(payments) - saleTotal}.
	 */
	public static NfceSale register(NfceSaleId id, PosSessionId sessionId, List<SaleItem> items,
			BigDecimal totalDiscount, List<Payment> payments, String customerCpf, Instant createdAt) {
		BigDecimal normalizedDiscount = totalDiscount == null ? BigDecimal.ZERO : totalDiscount;
		BigDecimal subtotal = sumLineTotals(items);
		if (normalizedDiscount.compareTo(subtotal) > 0) {
			throw new BusinessRuleException("totalDiscount (" + normalizedDiscount + ") cannot exceed the sale subtotal ("
					+ subtotal + ")");
		}
		BigDecimal saleTotal = subtotal.subtract(normalizedDiscount);
		BigDecimal paymentsSum = sumPayments(payments);
		if (paymentsSum.compareTo(saleTotal) < 0) {
			throw new BusinessRuleException(
					"Total payments (" + paymentsSum + ") do not cover the sale total (" + saleTotal + ")");
		}
		BigDecimal changeGiven = paymentsSum.subtract(saleTotal);
		return new NfceSale(id, sessionId, items, normalizedDiscount, payments, changeGiven, customerCpf,
				NfceSaleStatus.DRAFT, createdAt, null, null, null, null, false);
	}

	/**
	 * Reconstructs a sale from persistence, at any status in its lifecycle.
	 */
	public static NfceSale of(NfceSaleId id, PosSessionId sessionId, List<SaleItem> items, BigDecimal totalDiscount,
			List<Payment> payments, BigDecimal changeGiven, String customerCpf, NfceSaleStatus status,
			Instant createdAt, String documentSeries, Long documentNumber, String accessKey, String sefazProtocol,
			boolean contingencyMode) {
		return new NfceSale(id, sessionId, items, totalDiscount, payments, changeGiven, customerCpf, status, createdAt,
				documentSeries, documentNumber, accessKey, sefazProtocol, contingencyMode);
	}

	/**
	 * UC-M3-04, online path: SEFAZ-UF authorized the document in real time
	 * (AC1). Only a {@code DRAFT} sale can be issued — re-issuing an
	 * already-decided sale would allocate a second document number for the
	 * same commercial transaction.
	 */
	public NfceSale authorize(String documentSeries, Long documentNumber, String accessKey, String sefazProtocol) {
		requireDraft();
		return new NfceSale(id, sessionId, items, totalDiscount, payments, changeGiven, customerCpf,
				NfceSaleStatus.AUTHORIZED, createdAt, requireText(documentSeries, "documentSeries"),
				Objects.requireNonNull(documentNumber, "documentNumber is required"),
				Objects.requireNonNull(accessKey, "accessKey is required"), requireText(sefazProtocol, "sefazProtocol"),
				false);
	}

	/**
	 * UC-M3-04, contingency path (AC2): SEFAZ-UF was unreachable, so the sale
	 * is numbered and flagged for a later sync (UC-08) instead of blocking the
	 * cashier. No protocol is available yet.
	 */
	public NfceSale queueForContingency(String documentSeries, Long documentNumber, String accessKey) {
		requireDraft();
		return new NfceSale(id, sessionId, items, totalDiscount, payments, changeGiven, customerCpf,
				NfceSaleStatus.PENDING_SYNC, createdAt, requireText(documentSeries, "documentSeries"),
				Objects.requireNonNull(documentNumber, "documentNumber is required"),
				Objects.requireNonNull(accessKey, "accessKey is required"), null, true);
	}

	private void requireDraft() {
		if (status != NfceSaleStatus.DRAFT) {
			throw new BusinessRuleException("NfceSale " + id.value() + " is not DRAFT (current status: " + status + ")");
		}
	}

	private static String requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new BusinessRuleException(field + " is required");
		}
		return value;
	}

	public BigDecimal getSaleTotal() {
		return sumLineTotals(items).subtract(totalDiscount);
	}

	private static BigDecimal sumLineTotals(List<SaleItem> items) {
		return requireNonEmptyItems(items).stream().map(SaleItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static BigDecimal sumPayments(List<Payment> payments) {
		return requireNonEmptyPayments(payments).stream().map(Payment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static List<SaleItem> requireNonEmptyItems(List<SaleItem> items) {
		if (items == null || items.isEmpty()) {
			throw new BusinessRuleException("A sale must have at least one item");
		}
		return List.copyOf(items);
	}

	private static List<Payment> requireNonEmptyPayments(List<Payment> payments) {
		if (payments == null || payments.isEmpty()) {
			throw new BusinessRuleException("A sale must have at least one payment");
		}
		return List.copyOf(payments);
	}

	private static BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
		BigDecimal resolved = value == null ? BigDecimal.ZERO : value;
		if (resolved.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException(fieldName + " cannot be negative: " + resolved);
		}
		return resolved;
	}

	private static String normalizeCpf(String customerCpf) {
		if (customerCpf == null || customerCpf.isBlank()) {
			return null;
		}
		return Document.cpf(customerCpf).number();
	}
}
