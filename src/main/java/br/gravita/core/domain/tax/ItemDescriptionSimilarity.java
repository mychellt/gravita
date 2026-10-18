package br.gravita.core.domain.tax;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * How alike two product descriptions are, from 0 (nothing in common) to 1 (same words). Two suppliers rarely write the same
 * description for the same product, so this only ranks candidates for an operator to confirm; it never proves a match.
 */
public final class ItemDescriptionSimilarity {

	private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
	private static final Pattern NOT_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

	private ItemDescriptionSimilarity() {
	}

	/** Sørensen–Dice coefficient over the words of both descriptions, ignoring case, accents and punctuation. */
	public static double score(final String first, final String second) {
		final Set<String> firstWords = words(first);
		final Set<String> secondWords = words(second);
		if (firstWords.isEmpty() || secondWords.isEmpty()) {
			return 0;
		}
		final Set<String> shared = new HashSet<>(firstWords);
		shared.retainAll(secondWords);
		return 2.0 * shared.size() / (firstWords.size() + secondWords.size());
	}

	private static Set<String> words(final String text) {
		if (text == null) {
			return Set.of();
		}
		final String plain = DIACRITICS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("")
				.toLowerCase(Locale.ROOT);
		final Set<String> words = new HashSet<>(Arrays.asList(NOT_ALPHANUMERIC.split(plain)));
		words.remove("");
		return words;
	}
}
