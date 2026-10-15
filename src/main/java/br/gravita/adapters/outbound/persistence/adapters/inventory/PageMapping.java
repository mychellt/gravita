package br.gravita.adapters.outbound.persistence.adapters.inventory;

import br.gravita.core.domain.shared.Page;
import br.gravita.core.domain.shared.PageQuery;
import java.util.function.Function;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Translates between the core's page types and Spring Data's, for the inventory list queries. */
final class PageMapping {

	private PageMapping() {
	}

	static Pageable pageable(final PageQuery pageQuery, final Sort sort) {
		return PageRequest.of(pageQuery.page(), pageQuery.size(), sort);
	}

	static <E, D> Page<D> toPage(final org.springframework.data.domain.Page<E> result, final Function<E, D> mapper) {
		return new Page<>(result.getContent().stream().map(mapper).toList(), result.getNumber(), result.getSize(),
				result.getTotalElements());
	}
}
