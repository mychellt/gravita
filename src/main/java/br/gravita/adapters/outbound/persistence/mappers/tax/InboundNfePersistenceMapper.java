package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.InboundNfeConferenceItemEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.tax.InboundNfeItemEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.tax.InboundNfeJpaEntity;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeConferenceItem;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeTotals;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface InboundNfePersistenceMapper {

	@Mapping(target = "supplierDocument", source = "supplierDocument", qualifiedByName = "toSupplierDocument")
	@Mapping(target = "totals", source = "entity")
	InboundNfe map(final InboundNfeJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "companyId", source = "companyId.value")
	@Mapping(target = "supplierDocument", source = "supplierDocument.number")
	@Mapping(target = ".", source = "totals")
	InboundNfeJpaEntity map(final InboundNfe domain);

	InboundNfeTotals mapTotals(final InboundNfeJpaEntity entity);

	InboundNfeItem map(final InboundNfeItemEmbeddable embeddable);

	InboundNfeItemEmbeddable map(final InboundNfeItem item);

	InboundNfeConferenceItem map(final InboundNfeConferenceItemEmbeddable embeddable);

	InboundNfeConferenceItemEmbeddable map(final InboundNfeConferenceItem item);

	@Mapping(target = "value", source = "id")
	InboundNfeId mapInboundNfeId(final UUID id);

	@Mapping(target = "value", source = "id")
	CompanyId mapCompanyId(final UUID id);

	// The supplier of an inbound NFe is always a company, so the stored digits are re-validated as a CNPJ.
	@Named("toSupplierDocument")
	static Document toSupplierDocument(final String number) {
		return Document.cnpj(number);
	}
}
