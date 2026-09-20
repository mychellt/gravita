package br.gravita.adapters.outbound.persistence.entities;

import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.ProductType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Setter
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "products")
public class ProductJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String internalCode;

	@ElementCollection
	@CollectionTable(name = "product_barcodes", joinColumns = @JoinColumn(name = "product_id"))
	@Column(name = "barcode", nullable = false)
	private List<String> barcodes;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private ProductType type;

	private String ncm;

	private String cest;

	private Integer origin;

	@ElementCollection
	@CollectionTable(name = "product_default_cfop_by_operation", joinColumns = @JoinColumn(name = "product_id"))
	@MapKeyColumn(name = "operation")
	@Column(name = "cfop")
	private Map<String, String> defaultCfopByOperation;

	@ElementCollection
	@CollectionTable(name = "product_cst_csosn_by_state", joinColumns = @JoinColumn(name = "product_id"))
	@MapKeyColumn(name = "state")
	@Column(name = "cst_csosn")
	private Map<String, String> cstCsosnByState;

	@Embedded
	private ProductTaxProfileEmbeddable taxProfile;

	@Column(precision = 12, scale = 2)
	private BigDecimal averageCost;

	@Column(precision = 12, scale = 2)
	private BigDecimal basePrice;

	@Embedded
	private ProductStockEmbeddable stock;

	private String purchaseUnit;

	private String saleUnit;

	@Column(precision = 12, scale = 4)
	private BigDecimal conversionFactor;

	private Boolean lotControl;

	private Boolean serialControl;

	@Embedded
	private ProductClassificationEmbeddable classification;

	@ElementCollection
	@CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
	@Column(name = "image_url", nullable = false)
	private List<String> images;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private ProductStatus status;

	@ElementCollection
	@CollectionTable(name = "product_kit_components", joinColumns = @JoinColumn(name = "product_id"))
	private List<ProductKitComponentEmbeddable> kitComponents;

	@ElementCollection
	@CollectionTable(name = "product_variants", joinColumns = @JoinColumn(name = "product_id"))
	private List<ProductVariantEmbeddable> variants;
}
