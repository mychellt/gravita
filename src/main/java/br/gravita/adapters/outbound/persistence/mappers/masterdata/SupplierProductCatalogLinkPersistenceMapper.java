package br.gravita.adapters.outbound.persistence.mappers.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.SupplierProductCatalogLinkJpaEntity;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLinkId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface SupplierProductCatalogLinkPersistenceMapper {

	@Mapping(target = "id", source = "id", qualifiedByName = "toLinkId")
	@Mapping(target = "companyId", source = "companyId", qualifiedByName = "toCompanyId")
	@Mapping(target = "supplierId", source = "supplierId", qualifiedByName = "toSupplierId")
	SupplierProductCatalogLink map(final SupplierProductCatalogLinkJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "companyId", source = "companyId.value")
	@Mapping(target = "supplierId", source = "supplierId.value")
	SupplierProductCatalogLinkJpaEntity map(final SupplierProductCatalogLink domain);

	@Named("toLinkId")
	static SupplierProductCatalogLinkId toLinkId(final java.util.UUID id) {
		return SupplierProductCatalogLinkId.of(id);
	}

	@Named("toCompanyId")
	static CompanyId toCompanyId(final java.util.UUID id) {
		return CompanyId.of(id);
	}

	@Named("toSupplierId")
	static SupplierId toSupplierId(final java.util.UUID id) {
		return SupplierId.of(id);
	}
}
