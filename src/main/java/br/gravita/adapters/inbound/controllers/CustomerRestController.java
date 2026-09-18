package br.gravita.adapters.inbound.controllers;

import br.gravita.adapters.dtos.request.RegisterCustomerRequest;
import br.gravita.adapters.dtos.response.CustomerResponse;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.ports.business.CustomerRegistrationPort;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

	public CustomerRestController(CustomerRegistrationPort customerRegistrationPort) {
		this.customerRegistrationPort = customerRegistrationPort;
	}

	@PostMapping
	public ResponseEntity<CustomerResponse> register(@Valid @RequestBody RegisterCustomerRequest request) {
		CustomerDomain created = customerRegistrationPort.execute(new Context(request.toDomain()));
		return ResponseEntity.created(URI.create("/api/customers/" + created.getId())).body(CustomerResponse.from(created));
	}

	@GetMapping("/{id}")
	public ResponseEntity<CustomerResponse> findById(@PathVariable UUID id) {
		return ResponseEntity.ok().build();
	}
}
