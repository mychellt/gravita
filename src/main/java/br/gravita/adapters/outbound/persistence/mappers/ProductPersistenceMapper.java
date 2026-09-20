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

public class ProductPersistenceMapper {

	public ProductDomain toDomain(ProductJpaEntity entity) {
		return ProductDomain.builder()
				.id(entity.getId())
				.internalCode(entity.getInternalCode())
				.barcodes(entity.getBarcodes())
				.type(entity.getType())
				.ncm(entity.getNcm())
				.cest(entity.getCest())
				.origin(entity.getOrigin())
				.defaultCfopByOperation(entity.getDefaultCfopByOperation())
				.cstCsosnByState(entity.getCstCsosnByState())
				.taxProfile(toTaxProfileDomain(entity.getTaxProfile()))
				.averageCost(entity.getAverageCost())
				.basePrice(entity.getBasePrice())
				.stock(toStockDomain(entity.getStock()))
				.purchaseUnit(entity.getPurchaseUnit())
				.saleUnit(entity.getSaleUnit())
				.conversionFactor(entity.getConversionFactor())
				.lotControl(entity.getLotControl())
				.serialControl(entity.getSerialControl())
				.classification(toClassificationDomain(entity.getClassification()))
				.images(entity.getImages())
				.status(entity.getStatus())
				.kitComponents(entity.getKitComponents() == null ? null
						: entity.getKitComponents().stream().map(this::toKitComponentDomain).toList())
				.variants(entity.getVariants() == null ? null
						: entity.getVariants().stream().map(this::toVariantDomain).toList())
				.build();
	}

	public ProductJpaEntity toEntity(ProductDomain domain) {
		return ProductJpaEntity.builder()
				.id(domain.getId())
				.internalCode(domain.getInternalCode())
				.barcodes(domain.getBarcodes())
				.type(domain.getType())
				.ncm(domain.getNcm())
				.cest(domain.getCest())
				.origin(domain.getOrigin())
				.defaultCfopByOperation(domain.getDefaultCfopByOperation())
				.cstCsosnByState(domain.getCstCsosnByState())
				.taxProfile(toTaxProfileEmbeddable(domain.getTaxProfile()))
				.averageCost(domain.getAverageCost())
				.basePrice(domain.getBasePrice())
				.stock(toStockEmbeddable(domain.getStock()))
				.purchaseUnit(domain.getPurchaseUnit())
				.saleUnit(domain.getSaleUnit())
				.conversionFactor(domain.getConversionFactor())
				.lotControl(domain.getLotControl())
				.serialControl(domain.getSerialControl())
				.classification(toClassificationEmbeddable(domain.getClassification()))
				.images(domain.getImages())
				.status(domain.getStatus())
				.kitComponents(domain.getKitComponents() == null ? null
						: domain.getKitComponents().stream().map(this::toKitComponentEmbeddable).toList())
				.variants(domain.getVariants() == null ? null
						: domain.getVariants().stream().map(this::toVariantEmbeddable).toList())
				.build();
	}

	private TaxProfileDomain toTaxProfileDomain(ProductTaxProfileEmbeddable embeddable) {
		if (embeddable == null) {
			return null;
		}
		return new TaxProfileDomain(embeddable.getIcmsRate(), embeddable.getIpiRate(), embeddable.getPisRate(),
				embeddable.getCofinsRate(), embeddable.getIcmsStRate(), embeddable.getFcpRate());
	}

	private ProductTaxProfileEmbeddable toTaxProfileEmbeddable(TaxProfileDomain domain) {
		if (domain == null) {
			return null;
		}
		return new ProductTaxProfileEmbeddable(domain.icmsRate(), domain.ipiRate(), domain.pisRate(),
				domain.cofinsRate(), domain.icmsStRate(), domain.fcpRate());
	}

	private StockParametersDomain toStockDomain(ProductStockEmbeddable embeddable) {
		if (embeddable == null) {
			return null;
		}
		return new StockParametersDomain(embeddable.getMinimum(), embeddable.getMaximum(), embeddable.getReorderPoint());
	}

	private ProductStockEmbeddable toStockEmbeddable(StockParametersDomain domain) {
		if (domain == null) {
			return null;
		}
		return new ProductStockEmbeddable(domain.minimum(), domain.maximum(), domain.reorderPoint());
	}

	private ClassificationDomain toClassificationDomain(ProductClassificationEmbeddable embeddable) {
		if (embeddable == null) {
			return null;
		}
		return new ClassificationDomain(embeddable.getGroup(), embeddable.getSubgroup(), embeddable.getBrand(),
				embeddable.getSection());
	}

	private ProductClassificationEmbeddable toClassificationEmbeddable(ClassificationDomain domain) {
		if (domain == null) {
			return null;
		}
		return new ProductClassificationEmbeddable(domain.group(), domain.subgroup(), domain.brand(), domain.section());
	}

	private KitComponentDomain toKitComponentDomain(ProductKitComponentEmbeddable embeddable) {
		return new KitComponentDomain(embeddable.getProductId(), embeddable.getQuantity());
	}

	private ProductKitComponentEmbeddable toKitComponentEmbeddable(KitComponentDomain domain) {
		return new ProductKitComponentEmbeddable(domain.productId(), domain.quantity());
	}

	private ProductVariantDomain toVariantDomain(ProductVariantEmbeddable embeddable) {
		return new ProductVariantDomain(embeddable.getColor(), embeddable.getSize(), embeddable.getBarcode());
	}

	private ProductVariantEmbeddable toVariantEmbeddable(ProductVariantDomain domain) {
		return new ProductVariantEmbeddable(domain.color(), domain.size(), domain.barcode());
	}
}
