package br.gravita.system.adapter.out.persistence;

import br.gravita.system.domain.model.ApprovalModule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "approval_alcada")
public class ApprovalAlcadaJpaEntity {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, unique = true, length = 20)
	private ApprovalModule module;

	@Column(name = "threshold_value", precision = 14, scale = 4)
	private BigDecimal thresholdValue;

	@Column(name = "threshold_discount_percent", precision = 5, scale = 2)
	private BigDecimal thresholdDiscountPercent;

	@Column(name = "approver_profile_id", nullable = false)
	private UUID approverProfileId;

	@Column(name = "configured_at", nullable = false)
	private Instant configuredAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "modified_at", nullable = false)
	private Instant modifiedAt;
}
