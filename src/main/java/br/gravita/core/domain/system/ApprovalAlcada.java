package br.gravita.core.domain.system;

import br.gravita.core.domain.shared.BusinessRuleException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The single, per-module approval configuration that {@code purchasing}'s
 * {@code ApprovePurchaseOrderUseCase}, {@code sales}'s {@code ApproveSalesOrderUseCase} and
 * {@code finance}'s {@code ApprovePayableUseCase} resolve their approval requirement against
 * (doc §11.1, §7, §8.1, §9.2), instead of each maintaining its own threshold/approver.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalAlcada {

	private UUID id;
	private ApprovalModule module;
	private BigDecimal thresholdValue;
	private BigDecimal thresholdDiscountPercent;
	private UUID approverProfileId;
	private Instant configuredAt;

	public static ApprovalAlcada configure(ApprovalModule module, BigDecimal thresholdValue,
			BigDecimal thresholdDiscountPercent, ProfileReference approverProfile) {
		validate(thresholdValue, thresholdDiscountPercent, approverProfile);
		return ApprovalAlcada.builder()
				.id(UUID.randomUUID())
				.module(module)
				.thresholdValue(thresholdValue)
				.thresholdDiscountPercent(thresholdDiscountPercent)
				.approverProfileId(approverProfile.id())
				.configuredAt(Instant.now())
				.build();
	}

	public void reconfigure(BigDecimal thresholdValue, BigDecimal thresholdDiscountPercent,
			ProfileReference approverProfile) {
		validate(thresholdValue, thresholdDiscountPercent, approverProfile);
		this.thresholdValue = thresholdValue;
		this.thresholdDiscountPercent = thresholdDiscountPercent;
		this.approverProfileId = approverProfile.id();
		this.configuredAt = Instant.now();
	}

	private static void validate(BigDecimal thresholdValue, BigDecimal thresholdDiscountPercent,
			ProfileReference approverProfile) {
		if (thresholdValue == null && thresholdDiscountPercent == null) {
			throw new BusinessRuleException(
					"At least one of thresholdValue or thresholdDiscountPercent must be set");
		}
		if (thresholdValue != null && thresholdValue.signum() < 0) {
			throw new BusinessRuleException("thresholdValue cannot be negative");
		}
		if (thresholdDiscountPercent != null && thresholdDiscountPercent.signum() < 0) {
			throw new BusinessRuleException("thresholdDiscountPercent cannot be negative");
		}
		if (approverProfile == null) {
			throw new BusinessRuleException("approverProfile is required");
		}
	}
}
