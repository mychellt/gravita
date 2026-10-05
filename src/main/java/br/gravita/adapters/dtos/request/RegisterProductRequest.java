package br.gravita.adapters.dtos.request;

import br.gravita.core.domain.ClassificationDomain;
import br.gravita.core.domain.KitComponentDomain;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductType;
import br.gravita.core.domain.ProductVariantDomain;
import br.gravita.core.domain.StockParametersDomain;
import br.gravita.core.domain.TaxProfileDomain;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RegisterProductRequest(
		@NotBlank String internalCode,
		List<String> barcodes,
		@NotNull ProductType type,
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
		@Valid List<@Valid KitComponentRequest> kitComponents,
		@Valid List<@Valid VariantRequest> variants) {

	public record TaxProfileRequest(
			BigDecimal icmsRate,
			BigDecimal ipiRate,
			BigDecimal pisRate,
			BigDecimal cofinsRate,
			BigDecimal icmsStRate,
			BigDecimal fcpRate) {

		TaxProfileDomain toDomain() {
			return new TaxProfileDomain(icmsRate, ipiRate, pisRate, cofinsRate, icmsStRate, fcpRate);
		}
	}

	public record StockRequest(BigDecimal minimum, BigDecimal maximum, BigDecimal reorderPoint) {

		StockParametersDomain toDomain() {
			return new StockParametersDomain(minimum, maximum, reorderPoint);
		}
	}

	public record ClassificationRequest(String group, String subgroup, String brand, String section) {

		ClassificationDomain toDomain() {
			return new ClassificationDomain(group, subgroup, brand, section);
		}
	}

	public record KitComponentRequest(@NotNull UUID productId, @NotNull @Positive BigDecimal quantity) {

		KitComponentDomain toDomain() {
			return new KitComponentDomain(productId, quantity);
		}
	}

	public record VariantRequest(String color, String size, String barcode) {

		ProductVariantDomain toDomain() {
			return new ProductVariantDomain(color, size, barcode);
		}
	}

	public ProductDomain toDomain(final UUID id) {
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
				.kitComponents(kitComponents == null ? null : kitComponents.stream().map(KitComponentRequest::toDomain).toList())
				.variants(variants == null ? null : variants.stream().map(VariantRequest::toDomain).toList())
				.build();
	}
}
