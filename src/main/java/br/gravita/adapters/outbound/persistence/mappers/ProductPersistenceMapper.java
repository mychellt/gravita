package br.gravita.adapters.outbound.persistence.mappers;

import br.gravita.adapters.outbound.persistence.entities.ProductClassificationEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.ProductJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.ProductKitComponentEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.ProductStockEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.ProductTaxProfileEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.ProductVariantEmbeddable;
import br.gravita.core.domain.ClassificationDomain;
import br.gravita.core.domain.KitComponentDomain;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductVariantDomain;
import br.gravita.core.domain.StockParametersDomain;
import br.gravita.core.domain.TaxProfileDomain;
import org.mapstruct.Mapper;

@Mapper
public interface ProductPersistenceMapper {

    ProductDomain map(final ProductJpaEntity entity);

    ProductJpaEntity map(final ProductDomain domain);

    TaxProfileDomain map(final ProductTaxProfileEmbeddable embeddable);

    ProductTaxProfileEmbeddable map(final TaxProfileDomain domain);

    StockParametersDomain map(final ProductStockEmbeddable embeddable);

    ProductStockEmbeddable map(final StockParametersDomain domain);

    ClassificationDomain map(final ProductClassificationEmbeddable embeddable);

    ProductClassificationEmbeddable map(final ClassificationDomain domain);

    KitComponentDomain map(final ProductKitComponentEmbeddable embeddable);

    ProductKitComponentEmbeddable map(final KitComponentDomain domain);

    ProductVariantDomain map(final ProductVariantEmbeddable embeddable);

    ProductVariantEmbeddable map(final ProductVariantDomain domain);
}
