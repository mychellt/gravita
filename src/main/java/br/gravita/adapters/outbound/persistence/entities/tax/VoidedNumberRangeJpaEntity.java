package br.gravita.adapters.outbound.persistence.entities.tax;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * No {@code active}/{@code modifiedAt} columns and no update path in the
 * repository adapter - a {@code VoidedNumberRange} is immutable once
 * created (UC-M2-06's AC2), same as {@code AccessLogJpaEntity}.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "voided_number_ranges")
public class VoidedNumberRangeJpaEntity {

	@Id
	private UUID id;

	@Column(name = "company_id", nullable = false)
	private UUID companyId;

	@Column(nullable = false, length = 20)
	private String series;

	@Column(name = "start_number", nullable = false)
	private Long startNumber;

	@Column(name = "end_number", nullable = false)
	private Long endNumber;

	@Column(nullable = false, length = 500)
	private String justification;

	@Column(nullable = false, length = 100)
	private String protocol;

	@Column(name = "voided_at", nullable = false)
	private Instant voidedAt;
}
