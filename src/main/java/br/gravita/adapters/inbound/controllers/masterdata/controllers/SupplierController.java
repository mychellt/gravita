package br.gravita.adapters.inbound.controllers.masterdata.controllers;

import br.gravita.adapters.inbound.controllers.masterdata.dtos.RegisterSupplierRequest;
import br.gravita.adapters.inbound.controllers.masterdata.dtos.SupplierResponse;
import br.gravita.adapters.inbound.controllers.masterdata.dtos.UpdateSupplierRequest;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierNotFoundException;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.masterdata.RegisterSupplierUseCase;
import br.gravita.core.ports.inbound.masterdata.UpdateSupplierUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final RegisterSupplierUseCase registerSupplierUseCase;
    private final UpdateSupplierUseCase updateSupplierUseCase;

    @PostMapping
    public ResponseEntity<SupplierResponse> register(@Valid @RequestBody RegisterSupplierRequest request) {
        SupplierId id = registerSupplierUseCase.execute(request.toCommand());
        return ResponseEntity.created(URI.create("/api/suppliers/" + id.value())).body(SupplierResponse.from(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SupplierResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateSupplierRequest request) {
        SupplierId supplierId = SupplierId.of(id);
        updateSupplierUseCase.execute(request.toCommand(supplierId));
        return ResponseEntity.ok(SupplierResponse.from(supplierId));
    }

    @ExceptionHandler(SupplierNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleSupplierNotFoundException(SupplierNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
