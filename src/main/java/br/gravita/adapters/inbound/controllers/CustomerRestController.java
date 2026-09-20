package br.gravita.adapters.inbound.controllers;

import br.gravita.adapters.dtos.request.RegisterCustomerRequest;
import br.gravita.adapters.dtos.request.UpdateCustomerRequest;
import br.gravita.adapters.dtos.response.CustomerResponse;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.ports.business.CustomerRegistrationPort;
import br.gravita.core.ports.inbound.masterdata.UpdateCustomerUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
public class CustomerRestController {

	private final CustomerRegistrationPort customerRegistrationPort;
	private final UpdateCustomerUseCase updateCustomerUseCase;

	public CustomerRestController(CustomerRegistrationPort customerRegistrationPort, UpdateCustomerUseCase updateCustomerUseCase) {
		this.customerRegistrationPort = customerRegistrationPort;
		this.updateCustomerUseCase = updateCustomerUseCase;
	}

	@PostMapping
	public ResponseEntity<CustomerResponse> register(@Valid @RequestBody RegisterCustomerRequest request) {
		CustomerDomain created = customerRegistrationPort.execute(new Context(request.toDomain()));
		return ResponseEntity.created(URI.create("/api/customers/" + created.getId())).body(CustomerResponse.from(created));
	}

	@PatchMapping("/{id}")
	public ResponseEntity<Void> update(@PathVariable UUID id, @Valid @RequestBody UpdateCustomerRequest request) {
		updateCustomerUseCase.execute(request.toCommand(id));
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}")
	public ResponseEntity<CustomerResponse> findById(@PathVariable UUID id) {
		return ResponseEntity.ok().build();
	}
}
