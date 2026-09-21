package br.gravita.adapters.inbound.controllers.tax;

import br.gravita.core.domain.system.AccessLog;
import br.gravita.core.domain.system.AccessLogEvent;

import java.time.Instant;
import java.util.UUID;

public record AccessLogEntryResponse(
		UUID id, UUID userId, String email, AccessLogEvent event, boolean successful, String ip, String device, Instant timestamp) {

	public static AccessLogEntryResponse from(AccessLog accessLog) {
		return new AccessLogEntryResponse(
				accessLog.getId(),
				accessLog.getUserId() == null ? null : accessLog.getUserId().value(),
				accessLog.getEmail(),
				accessLog.getEvent(),
				accessLog.isSuccessful(),
				accessLog.getIp(),
				accessLog.getDevice(),
				accessLog.getTimestamp());
	}
}
