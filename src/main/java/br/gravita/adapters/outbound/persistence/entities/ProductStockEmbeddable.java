package br.gravita.adapters.outbound.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class ProductStockEmbeddable {
	@Column(name = "stock_minimum", precision = 12, scale = 3)
	private BigDecimal minimum;

	@Column(name = "stock_maximum", precision = 12, scale = 3)
	private BigDecimal maximum;

	@Column(name = "stock_reorder_point", precision = 12, scale = 3)
	private BigDecimal reorderPoint;
}
