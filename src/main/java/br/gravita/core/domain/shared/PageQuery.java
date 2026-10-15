package br.gravita.core.domain.shared;

/** Zero-based page and size of a list request; out-of-range values fall back to a safe window instead of failing. */
public record PageQuery(int page, int size) {

	public static final int DEFAULT_SIZE = 20;
	public static final int MAX_SIZE = 100;

	public PageQuery {
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
