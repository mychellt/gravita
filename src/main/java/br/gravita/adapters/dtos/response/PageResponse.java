package br.gravita.adapters.dtos.response;

import br.gravita.core.domain.shared.Page;
import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

	public static <S, T> PageResponse<T> from(final Page<S> page, final Function<S, T> mapper) {
		return new PageResponse<>(page.content().stream().map(mapper).toList(), page.page(), page.size(),
				page.totalElements(), page.totalPages());
	}
}
