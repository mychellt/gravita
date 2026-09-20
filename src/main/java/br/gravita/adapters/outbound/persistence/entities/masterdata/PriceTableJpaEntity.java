package br.gravita.adapters.outbound.persistence.entities.masterdata;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.masterdata.MaxDiscountBehavior;
import br.gravita.core.domain.masterdata.PriceFormation;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
@Table(name = "price_tables")
public class PriceTableJpaEntity extends AbstractEntity<UUID> {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "UUID")
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PriceFormation formation;

	@Column(name = "valid_from", nullable = false)
	private LocalDate validFrom;

	@Column(name = "valid_to")
	private LocalDate validTo;

	@Column(name = "max_discount_percent")
	private BigDecimal maxDiscountPercent;

	@Enumerated(EnumType.STRING)
	@Column(name = "max_discount_behavior", length = 10)
	private MaxDiscountBehavior maxDiscountBehavior;

	@ElementCollection
	@CollectionTable(name = "price_table_entries", joinColumns = @JoinColumn(name = "price_table_id"))
	private List<PriceTableEntryEmbeddable> entries;
}
