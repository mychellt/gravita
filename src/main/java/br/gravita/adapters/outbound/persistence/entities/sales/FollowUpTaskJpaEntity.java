package br.gravita.adapters.outbound.persistence.entities.sales;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.sales.AlertChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
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
@Table(name = "follow_up_tasks")
public class FollowUpTaskJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "opportunity_id")
	private UUID opportunityId;

	@Column(name = "customer_id")
	private UUID customerId;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@Column(nullable = false)
	private UUID owner;

	@Enumerated(EnumType.STRING)
	@Column(name = "alert_channel", nullable = false, length = 10)
	private AlertChannel alertChannel;
}
