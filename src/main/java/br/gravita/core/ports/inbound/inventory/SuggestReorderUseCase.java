package br.gravita.core.ports.inbound.inventory;

import java.util.List;

public interface SuggestReorderUseCase {
	List<ReorderSuggestion> execute(SuggestReorderQuery query);
}
