package br.gravita.adapters.outbound.persistence.mappers.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.SupplierProductCatalogLinkJpaEntity;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLinkId;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface SupplierProductCatalogLinkPersistenceMapper {

	SupplierProductCatalogLink map(final SupplierProductCatalogLinkJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "companyId", source = "companyId.value")
	@Mapping(target = "supplierId", source = "supplierId.value")
	SupplierProductCatalogLinkJpaEntity map(final SupplierProductCatalogLink domain);

	@Mapping(target = "value", source = "id")
	SupplierProductCatalogLinkId mapLinkId(final UUID id);

	@Mapping(target = "value", source = "id")
	CompanyId mapCompanyId(final UUID id);

	@Mapping(target = "value", source = "id")
	SupplierId mapSupplierId(final UUID id);
}
