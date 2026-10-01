package br.gravita.adapters.outbound.persistence.mappers.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.CostCenterShareEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.finance.PayableJpaEntity;
import br.gravita.core.domain.finance.CostCenterShare;
import br.gravita.core.domain.finance.LedgerScope;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PayablePersistenceMapper {

	@Mapping(target = "scope", source = "entity")
	Payable map(final PayableJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "companyId", source = "scope.companyId")
	@Mapping(target = "branchId", source = "scope.branchId")
	@Mapping(target = "bankAccountId", source = "scope.bankAccountId")
	PayableJpaEntity map(final Payable domain);

	LedgerScope mapScope(final PayableJpaEntity entity);

	CostCenterShare map(final CostCenterShareEmbeddable embeddable);

	CostCenterShareEmbeddable map(final CostCenterShare share);

	@Mapping(target = "value", source = "id")
	PayableId mapPayableId(final UUID id);
}
