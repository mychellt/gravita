package br.gravita.core.domain.system;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * One row per login attempt, successful or not (doc §11.1, UC-M10-05's write side; read/query
 * side is UC-M10-07). {@code userId} is null when the attempted e-mail doesn't match any {@link
 * User}, since AuthenticateUseCase must not reveal whether the e-mail exists - the log still
 * needs the attempted e-mail for security review.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccessLog {

	/** Retention window from doc §11.1: "mantido por 12 meses" (UC-M10-07). */
	public static final int RETENTION_MONTHS = 12;

	private UUID id;
	private UserId userId;
	private String email;
	private AccessLogEvent event;
	private boolean successful;
	private String ip;
	private String device;
	private Instant timestamp;

	public static AccessLog login(UserId userId, String email, boolean successful, String ip, String device) {
		return AccessLog.builder()
				.id(UUID.randomUUID())
				.userId(userId)
				.email(email)
				.event(AccessLogEvent.LOGIN)
				.successful(successful)
				.ip(ip)
				.device(device)
				.timestamp(Instant.now())
				.build();
	}
}
