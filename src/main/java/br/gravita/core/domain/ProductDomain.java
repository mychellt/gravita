package br.gravita.core.domain;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ProductDomain extends AbstractDomain {

	public static final int MAX_IMAGES = 5;

	private static final Pattern BARCODE_PATTERN = Pattern.compile("\\d{13}|\\d{14}");

	private String internalCode;
	private List<String> barcodes;
	private ProductType type;
	private String ncm;
	private String cest;
	private Integer origin;
	private Map<String, String> defaultCfopByOperation;
	private Map<String, String> cstCsosnByState;
	private TaxProfileDomain taxProfile;
	private BigDecimal averageCost;
	private BigDecimal basePrice;
	private StockParametersDomain stock;
	private String purchaseUnit;
	private String saleUnit;
	private BigDecimal conversionFactor;
	private Boolean lotControl;
	private Boolean serialControl;
	private ClassificationDomain classification;
	private List<String> images;
	private ProductStatus status;
	private List<KitComponentDomain> kitComponents;
	private List<ProductVariantDomain> variants;

	public void applyPartialUpdate(ProductDomain patch) {
		if (patch.internalCode != null) {
			this.internalCode = patch.internalCode;
		}
		if (patch.barcodes != null) {
			this.barcodes = patch.barcodes;
		}
		if (patch.type != null) {
			this.type = patch.type;
		}
		if (patch.ncm != null) {
			this.ncm = patch.ncm;
		}
		if (patch.cest != null) {
			this.cest = patch.cest;
		}
		if (patch.origin != null) {
			this.origin = patch.origin;
		}
		if (patch.defaultCfopByOperation != null) {
			this.defaultCfopByOperation = patch.defaultCfopByOperation;
		}
		if (patch.cstCsosnByState != null) {
			this.cstCsosnByState = patch.cstCsosnByState;
		}
		if (patch.taxProfile != null) {
			this.taxProfile = patch.taxProfile;
		}
		if (patch.averageCost != null) {
			this.averageCost = patch.averageCost;
		}
		if (patch.basePrice != null) {
			this.basePrice = patch.basePrice;
		}
		if (patch.stock != null) {
			this.stock = patch.stock;
		}
		if (patch.purchaseUnit != null) {
			this.purchaseUnit = patch.purchaseUnit;
		}
		if (patch.saleUnit != null) {
			this.saleUnit = patch.saleUnit;
		}
		if (patch.conversionFactor != null) {
			this.conversionFactor = patch.conversionFactor;
		}
		if (patch.lotControl != null) {
			this.lotControl = patch.lotControl;
		}
		if (patch.serialControl != null) {
			this.serialControl = patch.serialControl;
		}
		if (patch.classification != null) {
			this.classification = patch.classification;
		}
		if (patch.images != null) {
			this.images = patch.images;
		}
		if (patch.status != null) {
			this.status = patch.status;
		}
		if (patch.kitComponents != null) {
			this.kitComponents = patch.kitComponents;
		}
		if (patch.variants != null) {
			this.variants = patch.variants;
		}
	}

	public void validate() {
		validateImages();
		validateBarcodes();
		validateServiceHasNoStockFields();
		validateKitHasComponents();
		validateVariantHasGrid();
	}

	private void validateImages() {
		if (images != null && images.size() > MAX_IMAGES) {
			throw new BusinessRuleException("A product can have at most " + MAX_IMAGES + " images");
		}
	}

	private void validateBarcodes() {
		if (barcodes == null) {
			return;
		}
		for (String barcode : barcodes) {
			if (barcode == null || !BARCODE_PATTERN.matcher(barcode).matches()) {
				throw new BusinessRuleException("Barcode must be a valid EAN-13 or DUN-14 code: " + barcode);
			}
		}
	}

	private void validateServiceHasNoStockFields() {
		if (type != ProductType.SERVICE) {
			return;
		}
		if (stock != null || purchaseUnit != null || saleUnit != null) {
			throw new BusinessRuleException("Service products cannot carry stock parameters or units");
		}
	}

	private void validateKitHasComponents() {
		if (type != ProductType.KIT) {
			return;
		}
		if (kitComponents == null || kitComponents.isEmpty()) {
			throw new BusinessRuleException("Kit products must compose at least one existing product");
		}
	}

	private void validateVariantHasGrid() {
		if (type != ProductType.VARIANT) {
			return;
		}
		if (variants == null || variants.isEmpty()) {
			throw new BusinessRuleException("Variant products must define at least one color/size combination");
		}
	}
}
