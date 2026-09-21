package br.gravita.adapters.outbound.persistence.entities.tax;

import br.gravita.core.domain.system.AccessLogEvent;
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

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "access_log")
public class AccessLogJpaEntity {

	@Id
	private UUID id;

	@Column(name = "user_id")
	private UUID userId;

	@Column(nullable = false)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private AccessLogEvent event;

	@Column(nullable = false)
	private boolean successful;

	@Column(length = 64)
	private String ip;

	@Column(length = 255)
	private String device;

	@Column(nullable = false)
	private Instant timestamp;
}
