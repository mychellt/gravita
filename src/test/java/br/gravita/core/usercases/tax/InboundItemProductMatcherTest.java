package br.gravita.core.usercases.tax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.ports.inbound.tax.InboundNfeConference.MatchReason;
import br.gravita.core.ports.inbound.tax.InboundNfeConference.ProductSuggestion;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InboundItemProductMatcherTest {

	private static final String GTIN = "7908597504167";
	private static final String DESCRIPTION = "PISO BR BO AR IN 46X46 IPANEMA BG M2 Lote Tonalidade T25064B5";

	@Mock
	private ProductRepositoryPort productRepositoryPort;

	@InjectMocks
	private InboundItemProductMatcher matcher;

	@Test
	@DisplayName("a product with the same GTIN is the only suggestion, without ranking descriptions")
	void gtinMatchWinsOutright() {
		final ProductDomain product = product("PISO IPANEMA BEGE", ProductStatus.ACTIVE);
		when(productRepositoryPort.findByBarcode(GTIN)).thenReturn(Optional.of(product));

		final List<ProductSuggestion> suggestions = matcher.suggestFor(item(GTIN, "69072200"));

		assertThat(suggestions).containsExactly(new ProductSuggestion(product.getId(), "PISO IPANEMA BEGE", MatchReason.GTIN));
		verify(productRepositoryPort, never()).findAllByNcm(any());
	}

	@Test
	@DisplayName("without a GTIN match, ranks products of the same NCM by description, best first")
	void ranksSimilarProductsOfTheSameNcm() {
		final ProductDomain best = product("PISO BR BO AR IN 46X46 IPANEMA BG M2", ProductStatus.ACTIVE);
		final ProductDomain weaker = product("PISO 46X46 IPANEMA BG", ProductStatus.ACTIVE);
		final ProductDomain unrelated = product("REJUNTE FLEXIVEL CINZA 1KG", ProductStatus.ACTIVE);
		when(productRepositoryPort.findByBarcode(GTIN)).thenReturn(Optional.empty());
		when(productRepositoryPort.findAllByNcm("69072200")).thenReturn(List.of(weaker, unrelated, best));

		final List<ProductSuggestion> suggestions = matcher.suggestFor(item(GTIN, "69072200"));

		assertThat(suggestions).extracting(ProductSuggestion::productId).containsExactly(best.getId(), weaker.getId());
		assertThat(suggestions).extracting(ProductSuggestion::reason).containsOnly(MatchReason.SIMILAR_DESCRIPTION);
	}

	@Test
	@DisplayName("compares NCM digits only, so a dotted item NCM still finds the products")
	void comparesNcmDigitsOnly() {
		when(productRepositoryPort.findAllByNcm("69072200")).thenReturn(List.of());

		matcher.suggestFor(item(null, "6907.22.00"));

		verify(productRepositoryPort).findAllByNcm("69072200");
	}

	@Test
	@DisplayName("never suggests an inactive product")
	void ignoresInactiveProducts() {
		final ProductDomain inactiveGtinMatch = product("PISO IPANEMA", ProductStatus.INACTIVE);
		final ProductDomain inactiveSimilar = product(DESCRIPTION, ProductStatus.INACTIVE);
		when(productRepositoryPort.findByBarcode(GTIN)).thenReturn(Optional.of(inactiveGtinMatch));
		when(productRepositoryPort.findAllByNcm("69072200")).thenReturn(List.of(inactiveSimilar));

		assertThat(matcher.suggestFor(item(GTIN, "69072200"))).isEmpty();
	}

	@Test
	@DisplayName("offers at most three suggestions")
	void offersAtMostThreeSuggestions() {
		when(productRepositoryPort.findAllByNcm("69072200")).thenReturn(List.of(
				product(DESCRIPTION + " A", ProductStatus.ACTIVE), product(DESCRIPTION + " B", ProductStatus.ACTIVE),
				product(DESCRIPTION + " C", ProductStatus.ACTIVE), product(DESCRIPTION + " D", ProductStatus.ACTIVE)));

		assertThat(matcher.suggestFor(item(null, "69072200"))).hasSize(InboundItemProductMatcher.MAX_SUGGESTIONS);
	}

	@Test
	@DisplayName("suggests nothing for an item without GTIN and without NCM")
	void suggestsNothingWithoutAnyClue() {
		assertThat(matcher.suggestFor(item(null, null))).isEmpty();
		verify(productRepositoryPort, never()).findAllByNcm(any());
	}

	private static ProductDomain product(final String internalCode, final ProductStatus status) {
		return ProductDomain.builder().id(UUID.randomUUID()).internalCode(internalCode).status(status).build();
	}

	private static InboundNfeItem item(final String gtin, final String ncm) {
		return new InboundNfeItem("7908597504167", DESCRIPTION, ncm, "5102", "M2", new BigDecimal("2.3"),
				new BigDecimal("22.90"), new BigDecimal("52.67"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO, gtin, null);
	}
}
