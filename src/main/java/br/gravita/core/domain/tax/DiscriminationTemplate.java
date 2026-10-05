package br.gravita.core.domain.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import java.util.Objects;
import lombok.Getter;

/**
 * Reusable free-text for the discrimination field of an RPS/NFSe, keyed by service type. Several templates may exist
 * for the same service type. Only the text is copied into a document when it is used, so editing or deleting a
 * template never touches documents already issued.
 */
@Getter
public final class DiscriminationTemplate {

	private final DiscriminationTemplateId id;
	private ServiceCode serviceCode;
	private String templateText;

	public DiscriminationTemplate(final DiscriminationTemplateId id, final ServiceCode serviceCode, final String templateText) {
		this.id = Objects.requireNonNull(id, "id is required");
		apply(serviceCode, templateText);
	}

	public static DiscriminationTemplate of(final DiscriminationTemplateId id, final ServiceCode serviceCode,
			final String templateText) {
		return new DiscriminationTemplate(id, serviceCode, templateText);
	}

	public void update(final ServiceCode serviceCode, final String templateText) {
		apply(serviceCode, templateText);
	}

	private void apply(final ServiceCode serviceCode, final String templateText) {
		if (serviceCode == null) {
			throw new BusinessRuleException("serviceCode is required");
		}
		if (templateText == null || templateText.isBlank()) {
			throw new BusinessRuleException("templateText is required");
		}
		this.serviceCode = serviceCode;
		this.templateText = templateText;
	}
}
