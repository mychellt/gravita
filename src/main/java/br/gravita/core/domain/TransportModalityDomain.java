package br.gravita.core.domain;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.tax.TransportModality;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Arrays;
import java.util.Locale;

/**
 * A row of the Transport-Modalidade registry (the options of the NF-e form's Transporte > Modalidade select).
 * {@code code} is either a {@link TransportModality} name (CIF, FOB) or {@link #NONE_CODE} ("Sem transporte"),
 * so every registry row can be resolved when an NF-e is issued. NF-e documents keep the resolved modality, not a
 * reference to the row, which is why a row can be deactivated without touching any issued document.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class TransportModalityDomain extends AbstractDomain {
	/** "Sem transporte": no modality is sent with the NF-e. */
	public static final String NONE_CODE = "NONE";

	private String name;
	private String code;

	/** Upper-cases the code and checks it against the supported set; throws {@link BusinessRuleException} otherwise. */
	public static String normalizeCode(final String code) {
		final String normalized = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
		final boolean supported = NONE_CODE.equals(normalized)
				|| Arrays.stream(TransportModality.values()).anyMatch(modality -> modality.name().equals(normalized));
		if (!supported) {
			throw new BusinessRuleException("Unsupported transport modality code: " + code);
		}
		return normalized;
	}

	public boolean isEnabled() {
		return !Boolean.FALSE.equals(getActive());
	}
}
