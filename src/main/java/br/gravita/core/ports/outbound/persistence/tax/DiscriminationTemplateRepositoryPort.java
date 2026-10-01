package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.tax.DiscriminationTemplate;
import br.gravita.core.domain.tax.DiscriminationTemplateId;
import br.gravita.core.domain.tax.ServiceCode;
import java.util.List;
import java.util.Optional;

public interface DiscriminationTemplateRepositoryPort {

	DiscriminationTemplate save(DiscriminationTemplate template);

	Optional<DiscriminationTemplate> findById(DiscriminationTemplateId id);

	List<DiscriminationTemplate> findAll();

	List<DiscriminationTemplate> findByServiceCode(ServiceCode serviceCode);

	void deleteById(DiscriminationTemplateId id);
}
