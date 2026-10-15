package br.gravita.core.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.core.domain.shared.PageQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PageQueryTest {

	@Test
	@DisplayName("Keeps an in-range page and size as given")
	void keepsAnInRangePageAndSize() {
		final PageQuery query = new PageQuery(3, 50);

		assertThat(query.page()).isEqualTo(3);
		assertThat(query.size()).isEqualTo(50);
	}

	@Test
	@DisplayName("A negative page falls back to the first page")
	void negativePageFallsBackToTheFirstPage() {
		assertThat(new PageQuery(-1, 10).page()).isZero();
	}

	@Test
	@DisplayName("A non-positive size falls back to the default size")
	void nonPositiveSizeFallsBackToTheDefaultSize() {
		assertThat(new PageQuery(0, 0).size()).isEqualTo(PageQuery.DEFAULT_SIZE);
		assertThat(new PageQuery(0, -5).size()).isEqualTo(PageQuery.DEFAULT_SIZE);
	}

	@Test
	@DisplayName("A size above the maximum is capped")
	void sizeAboveTheMaximumIsCapped() {
		assertThat(new PageQuery(0, 5_000).size()).isEqualTo(PageQuery.MAX_SIZE);
	}
}
