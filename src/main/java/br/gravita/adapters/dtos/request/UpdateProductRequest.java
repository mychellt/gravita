package br.gravita.adapters.dtos.request;

import br.gravita.adapters.dtos.request.RegisterProductRequest.ClassificationRequest;
import br.gravita.adapters.dtos.request.RegisterProductRequest.KitComponentRequest;
import br.gravita.adapters.dtos.request.RegisterProductRequest.StockRequest;
import br.gravita.adapters.dtos.request.RegisterProductRequest.TaxProfileRequest;
import br.gravita.adapters.dtos.request.RegisterProductRequest.VariantRequest;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.ProductType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record UpdateProductRequest(
		String internalCode,
		List<String> barcodes,
		ProductType type,
		String ncm,
		String cest,
		@Min(0) @Max(8) Integer origin,
		Map<String, String> defaultCfopByOperation,
		Map<String, String> cstCsosnByState,
		@Valid TaxProfileRequest taxProfile,
		BigDecimal averageCost,
		BigDecimal basePrice,
		@Valid StockRequest stock,
		String purchaseUnit,
		String saleUnit,
		BigDecimal conversionFactor,
		Boolean lotControl,
		Boolean serialControl,
		@Valid ClassificationRequest classification,
		@Size(max = ProductDomain.MAX_IMAGES, message = "A product can have at most 5 images") List<String> images,
		ProductStatus status,
		@Valid List<@Valid KitComponentRequest> kitComponents,
		@Valid List<@Valid VariantRequest> variants) {

	public ProductDomain toDomain(UUID id) {
		return ProductDomain.builder()
				.id(id)
				.internalCode(internalCode)
				.barcodes(barcodes)
				.type(type)
				.ncm(ncm)
				.cest(cest)
				.origin(origin)
				.defaultCfopByOperation(defaultCfopByOperation)
				.cstCsosnByState(cstCsosnByState)
				.taxProfile(taxProfile == null ? null : taxProfile.toDomain())
				.averageCost(averageCost)
				.basePrice(basePrice)
				.stock(stock == null ? null : stock.toDomain())
				.purchaseUnit(purchaseUnit)
				.saleUnit(saleUnit)
				.conversionFactor(conversionFactor)
				.lotControl(lotControl)
				.serialControl(serialControl)
				.classification(classification == null ? null : classification.toDomain())
				.images(images)
				.status(status)
				.kitComponents(kitComponents == null ? null : kitComponents.stream().map(KitComponentRequest::toDomain).toList())
				.variants(variants == null ? null : variants.stream().map(VariantRequest::toDomain).toList())
				.build();
	}
}
