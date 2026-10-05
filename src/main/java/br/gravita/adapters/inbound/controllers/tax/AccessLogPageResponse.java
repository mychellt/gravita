package br.gravita.adapters.inbound.controllers.tax;

import br.gravita.core.domain.shared.Page;
import br.gravita.core.domain.system.AccessLog;

import java.util.List;

public record AccessLogPageResponse(List<AccessLogEntryResponse> content, int page, int size, long totalElements, int totalPages) {

	public static AccessLogPageResponse from(final Page<AccessLog> page) {
		return new AccessLogPageResponse(
				page.content().stream().map(AccessLogEntryResponse::from).toList(),
				page.page(),
				page.size(),
				page.totalElements(),
				page.totalPages());
	}
}
