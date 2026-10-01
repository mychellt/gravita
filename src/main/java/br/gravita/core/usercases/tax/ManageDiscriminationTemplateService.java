package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.tax.DiscriminationTemplate;
import br.gravita.core.domain.tax.DiscriminationTemplateId;
import br.gravita.core.domain.tax.ServiceCode;
import br.gravita.core.ports.inbound.tax.CreateDiscriminationTemplateCommand;
import br.gravita.core.ports.inbound.tax.ManageDiscriminationTemplateUseCase;
import br.gravita.core.ports.inbound.tax.UpdateDiscriminationTemplateCommand;
import br.gravita.core.ports.outbound.persistence.tax.DiscriminationTemplateRepositoryPort;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class ManageDiscriminationTemplateService implements ManageDiscriminationTemplateUseCase {

	private final DiscriminationTemplateRepositoryPort repositoryPort;

	public ManageDiscriminationTemplateService(DiscriminationTemplateRepositoryPort repositoryPort) {
		this.repositoryPort = repositoryPort;
	}

	@Override
	@Transactional
	public DiscriminationTemplateId create(CreateDiscriminationTemplateCommand command) {
		DiscriminationTemplate template = DiscriminationTemplate.of(DiscriminationTemplateId.of(UUID.randomUUID()),
				ServiceCode.of(command.serviceCode()), command.templateText());
		return repositoryPort.save(template).getId();
	}

	@Override
	@Transactional
	public void update(UpdateDiscriminationTemplateCommand command) {
		DiscriminationTemplate template = find(command.id());
		template.update(ServiceCode.of(command.serviceCode()), command.templateText());
		repositoryPort.save(template);
	}

	/**
	 * Only the template row goes away: a document copies the text when the template is used and keeps no reference to
	 * it, so already issued RPS/NFSe keep their discrimination.
	 */
	@Override
	@Transactional
	public void delete(DiscriminationTemplateId id) {
		find(id);
		repositoryPort.deleteById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public List<DiscriminationTemplate> list(ServiceCode serviceType) {
		return serviceType == null ? repositoryPort.findAll() : repositoryPort.findByServiceCode(serviceType);
	}

	private DiscriminationTemplate find(DiscriminationTemplateId id) {
		return repositoryPort.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Discrimination template not found: " + id.value()));
	}
}
