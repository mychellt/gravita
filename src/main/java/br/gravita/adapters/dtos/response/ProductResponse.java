package br.gravita.adapters.dtos.response;

import br.gravita.core.domain.ClassificationDomain;
import br.gravita.core.domain.KitComponentDomain;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.ProductType;
import br.gravita.core.domain.ProductVariantDomain;
import br.gravita.core.domain.StockParametersDomain;
import br.gravita.core.domain.TaxProfileDomain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductResponse(
		UUID id,
		String internalCode,
		List<String> barcodes,
		ProductType type,
		String ncm,
		String cest,
		Integer origin,
		Map<String, String> defaultCfopByOperation,
		Map<String, String> cstCsosnByState,
		TaxProfileDomain taxProfile,
		BigDecimal averageCost,
		BigDecimal basePrice,
		StockParametersDomain stock,
		String purchaseUnit,
		String saleUnit,
		BigDecimal conversionFactor,
		Boolean lotControl,
		Boolean serialControl,
		ClassificationDomain classification,
		List<String> images,
		ProductStatus status,
		List<KitComponentDomain> kitComponents,
		List<ProductVariantDomain> variants) {

	public static ProductResponse from(final ProductDomain domain) {
		return new ProductResponse(
				domain.getId(),
				domain.getInternalCode(),
				domain.getBarcodes(),
				domain.getType(),
				domain.getNcm(),
				domain.getCest(),
				domain.getOrigin(),
				domain.getDefaultCfopByOperation(),
				domain.getCstCsosnByState(),
				domain.getTaxProfile(),
				domain.getAverageCost(),
				domain.getBasePrice(),
				domain.getStock(),
				domain.getPurchaseUnit(),
				domain.getSaleUnit(),
				domain.getConversionFactor(),
				domain.getLotControl(),
				domain.getSerialControl(),
				domain.getClassification(),
				domain.getImages(),
				domain.getStatus(),
				domain.getKitComponents(),
				domain.getVariants());
	}
}
