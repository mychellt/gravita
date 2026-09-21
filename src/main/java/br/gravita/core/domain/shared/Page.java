package br.gravita.core.domain.shared;

import java.util.List;

public record Page<T>(List<T> content, int page, int size, long totalElements) {

	public int totalPages() {
		return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
	}
}
