package br.gravita.core.domain.tax;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ItemDescriptionSimilarityTest {

	@Test
	@DisplayName("is 1 for the same words, whatever the case, accents or punctuation")
	void isOneForTheSameWords() {
		assertThat(ItemDescriptionSimilarity.score("Piso Porcelanato 46x46", "PISO, PORCELANATO 46X46")).isEqualTo(1.0);
		assertThat(ItemDescriptionSimilarity.score("Açúcar Refinado 1kg", "acucar refinado 1KG")).isEqualTo(1.0);
	}

	@Test
	@DisplayName("is 0 when the descriptions share no word")
	void isZeroWhenNothingIsShared() {
		assertThat(ItemDescriptionSimilarity.score("Parafuso Sextavado M8", "Tinta Acrilica 20L")).isZero();
	}

	@Test
	@DisplayName("is between 0 and 1 for a partial overlap, higher the more words are shared")
	void growsWithTheSharedWords() {
		final String item = "PISO BR BO AR IN 46X46 IPANEMA BG M2 Lote Tonalidade T25064B5";

		final double closer = ItemDescriptionSimilarity.score(item, "PISO BR BO AR IN 46X46 IPANEMA BG M2");
		final double farther = ItemDescriptionSimilarity.score(item, "PISO 46X46 CINZA");

		assertThat(closer).isBetween(0.0, 1.0);
		assertThat(closer).isGreaterThan(farther);
		assertThat(farther).isGreaterThan(0.0);
	}

	@Test
	@DisplayName("is 0 when a description is missing or blank")
	void isZeroForMissingText() {
		assertThat(ItemDescriptionSimilarity.score(null, "Piso")).isZero();
		assertThat(ItemDescriptionSimilarity.score("Piso", "  ")).isZero();
	}
}
