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
import org.springframework.dao.DataIntegrityViolationException;
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
    public ResponseEntity<CustomerResponse> register(@AuthenticatedUser final UserId callerId,
            @Valid @RequestBody final RegisterCustomerRequest request) {
        final CustomerDomain created = customerRegistrationPort.execute(new Context(request.toDomain()).withCaller(callerId));
        return ResponseEntity.created(URI.create("/api/customers/" + created.getId())).body(CustomerResponse.from(created));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable final UUID id, @Valid @RequestBody final UpdateCustomerRequest request) {
        updateCustomerUseCase.execute(request.toCommand(id));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> findAll(@AuthenticatedUser final UserId callerId) {
        return ResponseEntity.ok(listCustomersPort.execute(new Context().withCaller(callerId)).stream().map(CustomerResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> findById(@AuthenticatedUser final UserId callerId, @PathVariable final UUID id) {
        return ResponseEntity.ok(CustomerResponse.from(findCustomerPort.execute(new Context(id).withCaller(callerId))));
    }

    /** The document is unique across customers; registering it twice is a conflict the caller can act on, not a server error. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateCustomer(final DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Já existe um cliente cadastrado com este documento."));
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleCustomerNotFoundException(final CustomerNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }
}
