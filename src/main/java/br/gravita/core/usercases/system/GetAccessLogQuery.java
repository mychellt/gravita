package br.gravita.core.usercases.system;

import br.gravita.core.domain.system.UserId;

import java.time.Instant;

public record GetAccessLogQuery(UserId userId, Instant dateFrom, Instant dateTo, String ip, String device, int page, int size) {

	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	public GetAccessLogQuery {
		if (page < 0) {
			page = 0;
		}
		if (size <= 0) {
			size = DEFAULT_SIZE;
		} else if (size > MAX_SIZE) {
			size = MAX_SIZE;
		}
	}
}
