package br.gravita.adapters.inbound.controllers;

import br.gravita.adapters.dtos.request.RegisterCustomerRequest;
import br.gravita.adapters.dtos.request.UpdateCustomerRequest;
import br.gravita.adapters.dtos.response.CustomerResponse;
import br.gravita.adapters.inbound.controllers.security.AuthenticatedUser;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.exceptions.CustomerNotFoundException;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.business.CustomerRegistrationPort;
import br.gravita.core.ports.business.FindCustomerPort;
import br.gravita.core.ports.business.ListCustomersPort;
import br.gravita.core.ports.inbound.masterdata.UpdateCustomerUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/customers")
public class CustomerRestController {

    private final CustomerRegistrationPort customerRegistrationPort;
    private final UpdateCustomerUseCase updateCustomerUseCase;
    private final FindCustomerPort findCustomerPort;
    private final ListCustomersPort listCustomersPort;

    @PostMapping
    public ResponseEntity<CustomerResponse> register(@AuthenticatedUser UserId callerId,
            @Valid @RequestBody RegisterCustomerRequest request) {
        CustomerDomain created = customerRegistrationPort.execute(new Context(request.toDomain()).withCaller(callerId));
        return ResponseEntity.created(URI.create("/api/customers/" + created.getId())).body(CustomerResponse.from(created));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable UUID id, @Valid @RequestBody UpdateCustomerRequest request) {
        updateCustomerUseCase.execute(request.toCommand(id));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> findAll(@AuthenticatedUser UserId callerId) {
        return ResponseEntity.ok(listCustomersPort.execute(new Context().withCaller(callerId)).stream().map(CustomerResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> findById(@AuthenticatedUser UserId callerId, @PathVariable UUID id) {
        return ResponseEntity.ok(CustomerResponse.from(findCustomerPort.execute(new Context(id).withCaller(callerId))));
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleCustomerNotFoundException(CustomerNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }
}
