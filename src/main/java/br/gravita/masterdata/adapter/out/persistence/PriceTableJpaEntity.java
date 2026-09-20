package br.gravita.masterdata.adapter.out.persistence;

import br.gravita.masterdata.domain.model.MaxDiscountBehavior;
import br.gravita.masterdata.domain.model.PriceFormation;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "price_tables")
public class PriceTableJpaEntity implements Persistable<UUID> {

	@Id
	private UUID id;

	// id is always application-assigned (ManagePriceTableService); the repository
	// adapter sets this from existsById before save so Spring Data picks
	// persist() over merge() for a row that doesn't exist yet.
	@Transient
	@Builder.Default
	private boolean isNew = true;

	@Override
	public boolean isNew() {
		return isNew;
	}

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

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "modified_at", nullable = false)
	private LocalDateTime modifiedAt;
}
