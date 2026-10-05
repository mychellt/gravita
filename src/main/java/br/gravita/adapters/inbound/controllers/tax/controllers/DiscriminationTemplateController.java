package br.gravita.adapters.inbound.controllers.tax.controllers;

import br.gravita.adapters.inbound.controllers.tax.dtos.CreateDiscriminationTemplateResponse;
import br.gravita.adapters.inbound.controllers.tax.dtos.DiscriminationTemplateRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.DiscriminationTemplateResponse;
import br.gravita.core.domain.tax.DiscriminationTemplateId;
import br.gravita.core.domain.tax.ServiceCode;
import br.gravita.core.ports.inbound.tax.ManageDiscriminationTemplateUseCase;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/nfse/discrimination-templates")
public class DiscriminationTemplateController {

	private final ManageDiscriminationTemplateUseCase manageDiscriminationTemplateUseCase;

	@PostMapping
	public ResponseEntity<CreateDiscriminationTemplateResponse> create(
			@Valid @RequestBody final DiscriminationTemplateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(CreateDiscriminationTemplateResponse
				.from(manageDiscriminationTemplateUseCase.create(request.toCreateCommand())));
	}

	/** {@code ?serviceCode=} narrows the list to one service type; without it every template is returned. */
	@GetMapping
	public ResponseEntity<List<DiscriminationTemplateResponse>> list(
			@RequestParam(required = false) final String serviceCode) {
		final ServiceCode serviceType = serviceCode == null || serviceCode.isBlank() ? null : ServiceCode.of(serviceCode);
		return ResponseEntity.ok(manageDiscriminationTemplateUseCase.list(serviceType).stream()
				.map(DiscriminationTemplateResponse::from).toList());
	}

	@PutMapping("/{id}")
	public ResponseEntity<Void> update(@PathVariable final UUID id,
			@Valid @RequestBody final DiscriminationTemplateRequest request) {
		manageDiscriminationTemplateUseCase.update(request.toUpdateCommand(DiscriminationTemplateId.of(id)));
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable final UUID id) {
		manageDiscriminationTemplateUseCase.delete(DiscriminationTemplateId.of(id));
		return ResponseEntity.noContent().build();
	}
}
