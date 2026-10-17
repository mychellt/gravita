package br.gravita.adapters.outbound.persistence.entities.masterdata;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "supplier_product_catalog_links")
public class SupplierProductCatalogLinkJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "company_id", nullable = false)
	private UUID companyId;

	@Column(name = "supplier_id", nullable = false)
	private UUID supplierId;

	@Column(name = "supplier_product_code", nullable = false)
	private String supplierProductCode;

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(name = "last_purchase_unit_value", nullable = false, precision = 14, scale = 4)
	private BigDecimal lastPurchaseUnitValue;

	@Column(name = "last_purchase_at", nullable = false)
	private Instant lastPurchaseAt;
}
