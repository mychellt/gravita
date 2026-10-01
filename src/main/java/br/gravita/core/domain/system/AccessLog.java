package br.gravita.core.domain.system;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class AccessLog {

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
