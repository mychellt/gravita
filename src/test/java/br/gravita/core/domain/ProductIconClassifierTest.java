package br.gravita.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductIconClassifierTest {

	private final ProductIconClassifier classifier = ProductIconClassifier.getDefault();

	@ParameterizedTest
	@CsvSource({
			"Tinta Acrílica Fosca 18L, PAINT",
			"Piso Porcelanato 60x60, FLOORING",
			"Cimento CP-II 50kg, CEMENT",
			"Torneira de Cozinha, PLUMBING",
			"Disjuntor Bipolar 32A, ELECTRICAL",
			"Furadeira de Impacto, TOOLS",
			"Parafuso Sextavado, FASTENERS",
			"Compensado Naval, WOOD",
			"Silicone Neutro, ADHESIVE",
			"Fechadura Externa, LOCKSMITH",
			"Capacete de Segurança, SAFETY",
			"Detergente Neutro, CLEANING",
			"Mangueira de Jardim, GARDEN",
			"Vaso Sanitário, SANITARY",
			"Produto Qualquer, DEFAULT"})
	@DisplayName("Classifies by keyword, ignoring accents, case and numbers")
	void classifiesByKeyword(final String description, final ProductIcon expected) {
		assertEquals(expected, classifier.classify(description, null));
	}

	@Test
	@DisplayName("The head noun wins over a modifier: paint for floors is paint, grout for floors is cement")
	void headNounWins() {
		assertEquals(ProductIcon.PAINT, classifier.classify("Tinta acrílica para piso", null));
		assertEquals(ProductIcon.CEMENT, classifier.classify("Rejunte para piso", null));
	}

	@Test
	@DisplayName("Matches plurals but not unrelated longer words")
	void matchesPluralsOnly() {
		assertEquals(ProductIcon.PAINT, classifier.classify("Tintas", null));
		assertEquals(ProductIcon.DEFAULT, classifier.classify("Calcinha", null));
	}

	@Test
	@DisplayName("Falls back to the NCM chapter when the text matches nothing")
	void fallsBackToNcm() {
		assertEquals(ProductIcon.PAINT, classifier.classify("Produto 123", "3208.10.10"));
		assertEquals(ProductIcon.DEFAULT, classifier.classify("Produto 123", "99999999"));
	}

	@Test
	@DisplayName("Uses the classification when the description is not enough")
	void usesClassification() {
		assertEquals(ProductIcon.FLOORING, classifier.classify("Modelo Carrara", null, "Pisos e Revestimentos"));
	}

	@Test
	@DisplayName("A manual icon survives a refresh; an automatic one is recomputed")
	void refreshRespectsManualIcon() {
		final ProductDomain manual = ProductDomain.builder().internalCode("Tinta").icon(ProductIcon.TOOLS)
				.iconSource(ProductIconSource.MANUAL).build();
		manual.refreshIcon();
		assertEquals(ProductIcon.TOOLS, manual.getIcon());

		final ProductDomain auto = ProductDomain.builder().internalCode("Tinta").icon(ProductIcon.TOOLS).iconSource(ProductIconSource.AUTO).build();
		auto.refreshIcon();
		assertEquals(ProductIcon.PAINT, auto.getIcon());
	}

	@Test
	@DisplayName("Products saved before icons existed are classified on read")
	void resolvesLegacyProducts() {
		assertEquals(ProductIcon.FLOORING, ProductDomain.builder().internalCode("Piso Laminado").build().resolvedIcon());
	}
}
