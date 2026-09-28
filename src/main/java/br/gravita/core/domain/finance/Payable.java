package br.gravita.core.domain.finance;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;

@Getter
public final class Payable {

	private final PayableId id;
	private final UUID supplierId;
	private final PayableOrigin origin;
	private final BigDecimal amount;
	private final LocalDate dueDate;
	private final List<CostCenterShare> costCenterSplit;
	private final PayableStatus status;
	private final UUID purchaseReceiptRef;
	private final Integer installmentNumber;
	private final Integer installments;

	private Payable(PayableId id, UUID supplierId, PayableOrigin origin, BigDecimal amount, LocalDate dueDate,
			List<CostCenterShare> costCenterSplit, PayableStatus status, UUID purchaseReceiptRef,
			Integer installmentNumber, Integer installments) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.supplierId = supplierId;
		this.origin = Objects.requireNonNull(origin, "origin is required");
		this.amount = requirePositive(amount);
		this.dueDate = Objects.requireNonNull(dueDate, "dueDate is required");
		this.costCenterSplit = requireValidSplit(costCenterSplit);
		this.status = Objects.requireNonNull(status, "status is required");
		this.purchaseReceiptRef = purchaseReceiptRef;
		this.installmentNumber = installmentNumber;
		this.installments = installments;
	}

	/**
	 * A one-off expense not tied to a purchase receipt. {@code supplierId} is
	 * optional (pure expenses such as rent have none), and so is
	 * {@code costCenterSplit}; when given, its percentages must add up to 100.
	 */
	public static Payable createManual(PayableId id, UUID supplierId, BigDecimal amount, LocalDate dueDate,
			List<CostCenterShare> costCenterSplit) {
		return new Payable(id, supplierId, PayableOrigin.MANUAL, amount, dueDate, costCenterSplit,
				PayableStatus.OPEN, null, null, null);
	}

	/**
	 * One installment of the payment terms of a confirmed purchase receipt.
	 * {@code installments} is the total number of installments of the receipt,
	 * {@code installmentNumber} (1-based) this title's position among them.
	 */
	public static Payable createFromPurchaseReceipt(PayableId id, UUID supplierId, UUID purchaseReceiptRef,
			BigDecimal amount, LocalDate dueDate, int installmentNumber, int installments) {
		Objects.requireNonNull(supplierId, "supplierId is required");
		Objects.requireNonNull(purchaseReceiptRef, "purchaseReceiptRef is required");
		if (installmentNumber < 1 || installmentNumber > installments) {
			throw new BusinessRuleException(
					"installmentNumber must be between 1 and " + installments + ": " + installmentNumber);
		}
		return new Payable(id, supplierId, PayableOrigin.PURCHASE_RECEIPT, amount, dueDate, null,
				PayableStatus.OPEN, purchaseReceiptRef, installmentNumber, installments);
	}

	public static Payable of(PayableId id, UUID supplierId, PayableOrigin origin, BigDecimal amount,
			LocalDate dueDate, List<CostCenterShare> costCenterSplit, PayableStatus status, UUID purchaseReceiptRef,
			Integer installmentNumber, Integer installments) {
		return new Payable(id, supplierId, origin, amount, dueDate, costCenterSplit, status, purchaseReceiptRef,
				installmentNumber, installments);
	}

	private static BigDecimal requirePositive(BigDecimal amount) {
		if (amount == null) {
			throw new BusinessRuleException("amount is required");
		}
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("amount must be positive: " + amount);
		}
		return amount;
	}

	private static List<CostCenterShare> requireValidSplit(List<CostCenterShare> split) {
		if (split == null || split.isEmpty()) {
			return List.of();
		}
		Set<UUID> costCenters = new HashSet<>();
		BigDecimal total = BigDecimal.ZERO;
		for (CostCenterShare share : split) {
			if (!costCenters.add(share.costCenterId())) {
				throw new BusinessRuleException("costCenterSplit repeats cost center " + share.costCenterId());
			}
			total = total.add(share.percent());
		}
		if (total.compareTo(new BigDecimal("100")) != 0) {
			throw new BusinessRuleException("costCenterSplit percentages must sum to 100: " + total);
		}
		return List.copyOf(split);
	}
}
