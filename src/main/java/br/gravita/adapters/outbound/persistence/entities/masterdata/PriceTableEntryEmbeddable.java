package br.gravita.adapters.outbound.persistence.entities.masterdata;

import br.gravita.core.domain.masterdata.ProductOrClassRefType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class PriceTableEntryEmbeddable {

	@Enumerated(EnumType.STRING)
	@Column(name = "ref_type", nullable = false, length = 20)
	private ProductOrClassRefType refType;

	@Column(name = "reference_id", nullable = false)
	private String referenceId;

	@Column(name = "entry_value", nullable = false)
	private BigDecimal value;
}
