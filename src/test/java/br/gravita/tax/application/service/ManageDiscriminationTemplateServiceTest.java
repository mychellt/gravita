package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.tax.DiscriminationTemplate;
import br.gravita.core.domain.tax.DiscriminationTemplateId;
import br.gravita.core.domain.tax.ServiceCode;
import br.gravita.core.ports.inbound.tax.CreateDiscriminationTemplateCommand;
import br.gravita.core.ports.inbound.tax.UpdateDiscriminationTemplateCommand;
import br.gravita.core.ports.outbound.persistence.tax.DiscriminationTemplateRepositoryPort;
import br.gravita.core.usercases.tax.ManageDiscriminationTemplateService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManageDiscriminationTemplateServiceTest {

	@Mock
	private DiscriminationTemplateRepositoryPort repositoryPort;

	private ManageDiscriminationTemplateService service;

	@BeforeEach
	void setUp() {
		service = new ManageDiscriminationTemplateService(repositoryPort);
		org.mockito.Mockito.lenient().when(repositoryPort.save(any()))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	private static DiscriminationTemplate template(String serviceCode, String text) {
		return DiscriminationTemplate.of(DiscriminationTemplateId.of(UUID.randomUUID()), ServiceCode.of(serviceCode),
				text);
	}

	@Test
	@DisplayName("Creates a template with a canonical service code")
	void ac1_createsATemplateWithACanonicalServiceCode() {
		DiscriminationTemplateId id = service.create(new CreateDiscriminationTemplateCommand("1.05", "Licenciamento"));

		ArgumentCaptor<DiscriminationTemplate> saved = ArgumentCaptor.forClass(DiscriminationTemplate.class);
		verify(repositoryPort).save(saved.capture());
		assertThat(saved.getValue().getId()).isEqualTo(id);
		assertThat(saved.getValue().getServiceCode().value()).isEqualTo("01.05");
		assertThat(saved.getValue().getTemplateText()).isEqualTo("Licenciamento");
	}

	@Test
	@DisplayName("Rejects creation with an invalid service code or blank text")
	void createRejectsAnInvalidServiceCodeOrBlankText() {
		assertThatThrownBy(() -> service.create(new CreateDiscriminationTemplateCommand("99.99", "x")))
				.isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> service.create(new CreateDiscriminationTemplateCommand("01.05", "  ")))
				.isInstanceOf(BusinessRuleException.class);
		verify(repositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Updates the existing template in place")
	void ac1_updatesTheExistingTemplateInPlace() {
		DiscriminationTemplate existing = template("01.05", "old");
		when(repositoryPort.findById(existing.getId())).thenReturn(Optional.of(existing));

		service.update(new UpdateDiscriminationTemplateCommand(existing.getId(), "08.01", "new"));

		verify(repositoryPort).save(existing);
		assertThat(existing.getServiceCode().value()).isEqualTo("08.01");
		assertThat(existing.getTemplateText()).isEqualTo("new");
	}

	@Test
	@DisplayName("Reports not found when updating an unknown template")
	void updateOfAnUnknownTemplateIsNotFound() {
		DiscriminationTemplateId id = DiscriminationTemplateId.of(UUID.randomUUID());
		when(repositoryPort.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(new UpdateDiscriminationTemplateCommand(id, "01.05", "x")))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(repositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Deletes an existing template")
	void ac1_deletesAnExistingTemplate() {
		DiscriminationTemplate existing = template("01.05", "x");
		when(repositoryPort.findById(existing.getId())).thenReturn(Optional.of(existing));

		service.delete(existing.getId());

		verify(repositoryPort).deleteById(existing.getId());
	}

	@Test
	@DisplayName("Reports not found when deleting an unknown template")
	void deleteOfAnUnknownTemplateIsNotFound() {
		DiscriminationTemplateId id = DiscriminationTemplateId.of(UUID.randomUUID());
		when(repositoryPort.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ResourceNotFoundException.class);
		verify(repositoryPort, never()).deleteById(any());
	}

	@Test
	@DisplayName("Lists only the requested service type, or everything when none is given")
	void ac2_listsOnlyTheRequestedServiceTypeOrEverythingWhenNoneIsGiven() {
		List<DiscriminationTemplate> forType = List.of(template("01.05", "a"));
		List<DiscriminationTemplate> all = List.of(template("01.05", "a"), template("08.01", "b"));
		when(repositoryPort.findByServiceCode(ServiceCode.of("01.05"))).thenReturn(forType);
		when(repositoryPort.findAll()).thenReturn(all);

		assertThat(service.list(ServiceCode.of("1.05"))).isEqualTo(forType);
		assertThat(service.list(null)).isEqualTo(all);
	}
}
