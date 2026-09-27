package br.gravita.adapters.outbound.persistence.entities.sales;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.sales.FollowUpTarget;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "follow_up_rules")
public class FollowUpRuleJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "days_without_contact", nullable = false)
	private int daysWithoutContact;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private FollowUpTarget target;

	@Column(name = "notify_owner", nullable = false)
	private boolean notifyOwner;
}
