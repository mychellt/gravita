package br.gravita.core.domain.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service code of the LC 116/2003 list in canonical {@code II.SS} form (item 01-40, subitem 01-99), e.g.
 * {@code 01.05}. Accepts {@code 1.05}, {@code 0105} and {@code 01.05} as input. This is the structural half of the
 * LC 116 check; whether a municipality accepts the code is a separate, data-driven check (its municipal list).
 */
public record ServiceCode(String value) {

	private static final Pattern FORMAT = Pattern.compile("^(\\d{1,2})\\.?(\\d{2})$");
	private static final int MAX_ITEM = 40;

	public static ServiceCode of(final String raw) {
		if (raw == null || raw.isBlank()) {
			throw new BusinessRuleException("serviceCode is required");
		}
		final Matcher matcher = FORMAT.matcher(raw.trim());
		if (!matcher.matches()) {
			throw new BusinessRuleException("serviceCode is not in the LC 116/2003 format (e.g. 01.05): " + raw);
		}
		final int item = Integer.parseInt(matcher.group(1));
		final int subitem = Integer.parseInt(matcher.group(2));
		if (item < 1 || item > MAX_ITEM || subitem < 1) {
			throw new BusinessRuleException("serviceCode is not in the LC 116/2003 list: " + raw);
		}
		return new ServiceCode(String.format("%02d.%02d", item, subitem));
	}
}
