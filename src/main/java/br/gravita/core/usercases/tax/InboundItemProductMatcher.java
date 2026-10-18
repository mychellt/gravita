package br.gravita.core.usercases.tax;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.ItemDescriptionSimilarity;
import br.gravita.core.ports.inbound.tax.InboundNfeConference.MatchReason;
import br.gravita.core.ports.inbound.tax.InboundNfeConference.ProductSuggestion;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Finds the catalog products an NF-e item that has no supplier link probably stands for, so a purchase from a second
 * supplier reuses the product instead of registering it again. A GTIN match wins outright; without one, products of the
 * same NCM are ranked by how alike their description is. These are suggestions for the operator to confirm, never links.
 */
@Component
public class InboundItemProductMatcher {

	static final double MIN_DESCRIPTION_SCORE = 0.4;
	static final int MAX_SUGGESTIONS = 3;

	private final ProductRepositoryPort productRepositoryPort;

	public InboundItemProductMatcher(final ProductRepositoryPort productRepositoryPort) {
		this.productRepositoryPort = productRepositoryPort;
	}

	public List<ProductSuggestion> suggestFor(final InboundNfeItem item) {
		final Optional<ProductSuggestion> byGtin = suggestByGtin(item);
		if (byGtin.isPresent()) {
			return List.of(byGtin.get());
		}
		return suggestBySimilarDescription(item);
	}

	private Optional<ProductSuggestion> suggestByGtin(final InboundNfeItem item) {
		if (item.barcode() == null) {
			return Optional.empty();
		}
		return productRepositoryPort.findByBarcode(item.barcode())
				.filter(InboundItemProductMatcher::isUsable)
				.map(product -> suggestion(product, MatchReason.GTIN));
	}

	private List<ProductSuggestion> suggestBySimilarDescription(final InboundNfeItem item) {
		final String ncmDigits = item.ncm() == null ? "" : item.ncm().replaceAll("\\D", "");
		if (ncmDigits.isEmpty()) {
			return List.of();
		}
		return productRepositoryPort.findAllByNcm(ncmDigits).stream()
				.filter(InboundItemProductMatcher::isUsable)
				.map(product -> new Scored(product, ItemDescriptionSimilarity.score(item.description(), product.getInternalCode())))
				.filter(scored -> scored.score() >= MIN_DESCRIPTION_SCORE)
				.sorted(Comparator.comparingDouble(Scored::score).reversed())
				.limit(MAX_SUGGESTIONS)
				.map(scored -> suggestion(scored.product(), MatchReason.SIMILAR_DESCRIPTION))
				.toList();
	}

	private static boolean isUsable(final ProductDomain product) {
		return product.getStatus() != ProductStatus.INACTIVE;
	}

	private static ProductSuggestion suggestion(final ProductDomain product, final MatchReason reason) {
		return new ProductSuggestion(product.getId(), product.getInternalCode(), reason);
	}

	private record Scored(ProductDomain product, double score) {
	}
}
