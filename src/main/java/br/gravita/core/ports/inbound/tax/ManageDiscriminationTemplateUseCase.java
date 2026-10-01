package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.DiscriminationTemplate;
import br.gravita.core.domain.tax.DiscriminationTemplateId;
import br.gravita.core.domain.tax.ServiceCode;
import java.util.List;

public interface ManageDiscriminationTemplateUseCase {

	DiscriminationTemplateId create(CreateDiscriminationTemplateCommand command);

	void update(UpdateDiscriminationTemplateCommand command);

	void delete(DiscriminationTemplateId id);

	/** The templates of one service type, or all of them when {@code serviceType} is {@code null}. */
	List<DiscriminationTemplate> list(ServiceCode serviceType);
}
