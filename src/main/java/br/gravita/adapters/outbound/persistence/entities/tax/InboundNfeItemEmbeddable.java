package br.gravita.adapters.outbound.persistence.entities.tax;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InboundNfeItemEmbeddable {

	@Column(name = "supplier_product_code", nullable = false)
	private String supplierProductCode;

	@Column(name = "description", nullable = false)
	private String description;

	@Column(name = "ncm")
	private String ncm;

	@Column(name = "cfop")
	private String cfop;

	@Column(name = "unit")
	private String unit;

	@Column(name = "quantity", nullable = false)
	private BigDecimal quantity;

	@Column(name = "unit_value", nullable = false)
	private BigDecimal unitValue;

	@Column(name = "total_value", nullable = false)
	private BigDecimal totalValue;

	@Column(name = "icms_value", nullable = false)
	private BigDecimal icmsValue;

	@Column(name = "ipi_value", nullable = false)
	private BigDecimal ipiValue;

	@Column(name = "pis_value", nullable = false)
	private BigDecimal pisValue;

	@Column(name = "cofins_value", nullable = false)
	private BigDecimal cofinsValue;
}
