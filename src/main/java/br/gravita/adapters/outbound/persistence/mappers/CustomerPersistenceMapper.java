package br.gravita.adapters.outbound.persistence.mappers;

import br.gravita.adapters.outbound.persistence.entities.CustomerAddressEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.CustomerContactEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.CustomerJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.CustomerPriceTableEmbeddable;
import br.gravita.core.domain.AddressDomain;
import br.gravita.core.domain.ContactDomain;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerPriceTableLink;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;

@Mapper(imports = br.gravita.core.domain.shared.Document.class,
        nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface CustomerPersistenceMapper {

    @Mapping(target = "documentDomain", expression = "java(new Document(entity.getDocument(), entity.getPersonType()))")
    CustomerDomain map(final CustomerJpaEntity entity);

    @Mapping(target = "document", source = "documentDomain.number")
    @Mapping(target = "personType", source = "documentDomain.personType")
    CustomerJpaEntity map(final CustomerDomain domain);

    @Mapping(target = "isDefault", source = "default")
    AddressDomain map(final CustomerAddressEmbeddable embeddable);

    @Mapping(target = "isDefault", source = "default")
    CustomerAddressEmbeddable map(final AddressDomain domain);

    ContactDomain map(final CustomerContactEmbeddable embeddable);

    CustomerContactEmbeddable map(final ContactDomain domain);

    CustomerPriceTableLink map(final CustomerPriceTableEmbeddable embeddable);

    CustomerPriceTableEmbeddable map(final CustomerPriceTableLink domain);
}
