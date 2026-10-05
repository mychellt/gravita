package br.gravita.core.domain.system;

import br.gravita.core.domain.shared.BusinessRuleException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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

    public static ApprovalAlcada configure(final ApprovalModule module,
                                           final BigDecimal thresholdValue,
                                           final BigDecimal thresholdDiscountPercent,
                                           final ProfileReference approverProfile) {

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

    public void reconfigure(final BigDecimal thresholdValue, final BigDecimal thresholdDiscountPercent,
                            final ProfileReference approverProfile) {
        validate(thresholdValue, thresholdDiscountPercent, approverProfile);
        this.thresholdValue = thresholdValue;
        this.thresholdDiscountPercent = thresholdDiscountPercent;
        this.approverProfileId = approverProfile.id();
        this.configuredAt = Instant.now();
    }

    private static void validate(final BigDecimal thresholdValue, final BigDecimal thresholdDiscountPercent,
                                 final ProfileReference approverProfile) {
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
