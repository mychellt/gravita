package br.gravita.core.domain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Picks a product icon from its text and NCM. Rules live in {@code product-icon-rules.txt} as
 * {@code ICON | ncm prefixes | keywords}; line order is the tie-break priority.
 *
 * <p>Score per category: each keyword token scores {@value #KEYWORD_WEIGHT}; the first token (the head noun in Portuguese, e.g. "Tinta
 * acrílica para piso") gets {@value #HEAD_BONUS} extra; tokens from the classification score {@value #CLASSIFICATION_WEIGHT}. The NCM only
 * decides when the text matches nothing, since descriptions are what users actually see.
 */
public final class ProductIconClassifier {

	private static final String RULES_RESOURCE = "/product-icon-rules.txt";
	private static final int KEYWORD_WEIGHT = 2;
	private static final int HEAD_BONUS = 2;
	private static final int CLASSIFICATION_WEIGHT = 1;
	private static final int MAX_SUFFIX = 3;
	private static final int MIN_PREFIX_KEYWORD = 4;
	private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	private static final Pattern NON_LETTERS = Pattern.compile("[^a-z]+");
	private static final ProductIconClassifier DEFAULT_INSTANCE = new ProductIconClassifier(loadRules());

	private final List<Rule> rules;

	ProductIconClassifier(final List<Rule> rules) {
		this.rules = List.copyOf(rules);
	}

	public static ProductIconClassifier getDefault() {
		return DEFAULT_INSTANCE;
	}

	public ProductIcon classify(final String description, final String ncm, final String... classification) {
		final Map<ProductIcon, Integer> scores = new EnumMap<>(ProductIcon.class);
		final List<String> tokens = tokenize(description);
		for (int i = 0; i < tokens.size(); i++) {
			final int weight = KEYWORD_WEIGHT + (i == 0 ? HEAD_BONUS : 0);
			scoreToken(tokens.get(i), weight, scores);
		}
		for (final String text : classification) {
			tokenize(text).forEach(token -> scoreToken(token, CLASSIFICATION_WEIGHT, scores));
		}
		ProductIcon best = null;
		int bestScore = 0;
		for (final Rule rule : rules) {
			final int score = scores.getOrDefault(rule.icon(), 0);
			if (score > bestScore) {
				best = rule.icon();
				bestScore = score;
			}
		}
		return best != null ? best : classifyByNcm(ncm);
	}

	private void scoreToken(final String token, final int weight, final Map<ProductIcon, Integer> scores) {
		for (final Rule rule : rules) {
			if (rule.matches(token)) {
				scores.merge(rule.icon(), weight, Integer::sum);
			}
		}
	}

	private ProductIcon classifyByNcm(final String ncm) {
		final String digits = ncm == null ? "" : ncm.replaceAll("\\D", "");
		if (digits.isEmpty()) {
			return ProductIcon.DEFAULT;
		}
		return rules.stream()
				.filter(rule -> rule.ncmPrefixes().stream().anyMatch(digits::startsWith))
				.findFirst()
				.map(Rule::icon)
				.orElse(ProductIcon.DEFAULT);
	}

	static List<String> tokenize(final String text) {
		if (text == null) {
			return List.of();
		}
		final String plain = DIACRITICS.matcher(Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFD)).replaceAll("");
		final List<String> tokens = new ArrayList<>();
		for (final String token : NON_LETTERS.split(plain)) {
			if (!token.isEmpty()) {
				tokens.add(token);
			}
		}
		return tokens;
	}

	static List<Rule> parse(final List<String> lines) {
		final List<Rule> parsed = new ArrayList<>();
		for (final String line : lines) {
			if (line.isBlank() || line.startsWith("#")) {
				continue;
			}
			final String[] parts = line.split("\\|", -1);
			if (parts.length != 3) {
				throw new IllegalStateException("Invalid product icon rule: " + line);
			}
			final List<String> ncm = parts[1].isBlank() ? List.of() : List.of(parts[1].trim().split("\\s+"));
			final List<String> keywords = tokenize(parts[2]);
			parsed.add(new Rule(ProductIcon.valueOf(parts[0].trim()), ncm, keywords));
		}
		return parsed;
	}

	private static List<Rule> loadRules() {
		try (InputStream in = ProductIconClassifier.class.getResourceAsStream(RULES_RESOURCE)) {
			if (in == null) {
				throw new IllegalStateException("Missing " + RULES_RESOURCE);
			}
			final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			return parse(reader.lines().toList());
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	record Rule(ProductIcon icon, List<String> ncmPrefixes, List<String> keywords) {

		/** Exact word, its plural, or (for keywords of 4+ letters) a short derivation such as "tintas" or "pisos". */
		boolean matches(final String token) {
			return keywords.stream().anyMatch(keyword -> token.equals(keyword) || token.equals(keyword + "s")
					|| keyword.length() >= MIN_PREFIX_KEYWORD && token.startsWith(keyword) && token.length() <= keyword.length() + MAX_SUFFIX);
		}
	}
}
