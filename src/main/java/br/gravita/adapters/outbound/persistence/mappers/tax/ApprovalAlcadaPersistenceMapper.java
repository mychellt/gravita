package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.ApprovalAlcadaJpaEntity;
import br.gravita.core.domain.system.ApprovalAlcada;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface ApprovalAlcadaPersistenceMapper {

    // ApprovalAlcada exposes no setters (immutable outside its validating
    // constructor/builder), so disableBuilder leaves MapStruct with no write
    // accessor to target - build it by hand instead.
    default ApprovalAlcada map(final ApprovalAlcadaJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return ApprovalAlcada.builder()
                .id(entity.getId())
                .module(entity.getModule())
                .thresholdValue(entity.getThresholdValue())
                .thresholdDiscountPercent(entity.getThresholdDiscountPercent())
                .approverProfileId(entity.getApproverProfileId())
                .configuredAt(entity.getConfiguredAt())
                .build();
    }

    ApprovalAlcadaJpaEntity map(final ApprovalAlcada domain);
}
