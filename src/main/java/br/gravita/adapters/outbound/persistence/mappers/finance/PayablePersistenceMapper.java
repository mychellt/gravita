package br.gravita.adapters.outbound.persistence.mappers.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.CostCenterShareEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.finance.PayableJpaEntity;
import br.gravita.core.domain.finance.CostCenterShare;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import java.util.ArrayList;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PayablePersistenceMapper {

	default Payable toDomain(final PayableJpaEntity entity) {
		return Payable.of(PayableId.of(entity.getId()), entity.getSupplierId(), entity.getOrigin(),
				entity.getAmount(), entity.getDueDate(),
				entity.getCostCenterSplit().stream()
						.map(share -> new CostCenterShare(share.getCostCenterId(), share.getPercent())).toList(),
				entity.getStatus());
	}

	default PayableJpaEntity toEntity(final Payable domain) {
		return PayableJpaEntity.builder()
				.id(domain.getId() == null ? null : domain.getId().value())
				.supplierId(domain.getSupplierId())
				.origin(domain.getOrigin())
				.amount(domain.getAmount())
				.dueDate(domain.getDueDate())
				.costCenterSplit(new ArrayList<>(domain.getCostCenterSplit().stream()
						.map(share -> new CostCenterShareEmbeddable(share.costCenterId(), share.percent()))
						.toList()))
				.status(domain.getStatus())
				.build();
	}
}
