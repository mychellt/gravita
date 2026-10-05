package br.gravita.adapters.inbound.controllers.purchasing.controllers;

import br.gravita.adapters.inbound.controllers.purchasing.dtos.RegisterQuotationResponseRequest;
import br.gravita.core.domain.purchasing.QuotationNotFoundException;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.purchasing.RegisterQuotationResponseUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/purchasing/quotations")
public class QuotationController {

    private final RegisterQuotationResponseUseCase registerQuotationResponseUseCase;

    @PostMapping("/{id}/responses")
    public ResponseEntity<Void> registerResponse(@PathVariable final UUID id,
                                                 @Valid @RequestBody final RegisterQuotationResponseRequest request) {
        registerQuotationResponseUseCase.execute(request.toCommand(id));
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(QuotationNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleQuotationNotFoundException(final QuotationNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, String>> handleBusinessRuleException(final BusinessRuleException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
