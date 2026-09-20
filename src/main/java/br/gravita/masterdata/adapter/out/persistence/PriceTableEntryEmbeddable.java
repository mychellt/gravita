package br.gravita.masterdata.adapter.out.persistence;

import br.gravita.masterdata.domain.model.ProductOrClassRefType;
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

	@Column(nullable = false)
	private BigDecimal value;
}
