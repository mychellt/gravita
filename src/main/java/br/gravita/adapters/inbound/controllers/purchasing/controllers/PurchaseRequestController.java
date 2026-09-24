package br.gravita.adapters.inbound.controllers.purchasing.controllers;

import br.gravita.adapters.inbound.controllers.purchasing.dtos.CreatePurchaseRequestRequest;
import br.gravita.adapters.inbound.controllers.purchasing.dtos.PurchaseRequestResponse;
import br.gravita.adapters.inbound.controllers.purchasing.dtos.SendQuotationRequest;
import br.gravita.adapters.inbound.controllers.purchasing.dtos.SendQuotationResponse;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.purchasing.PurchaseRequestNotFoundException;
import br.gravita.core.domain.purchasing.QuotationId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.purchasing.CreatePurchaseRequestUseCase;
import br.gravita.core.ports.inbound.purchasing.SendQuotationUseCase;
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
@RequestMapping("/api/purchasing/requests")
public class PurchaseRequestController {

    private final CreatePurchaseRequestUseCase createPurchaseRequestUseCase;
    private final SendQuotationUseCase sendQuotationUseCase;

    @PostMapping
    public ResponseEntity<PurchaseRequestResponse> create(@Valid @RequestBody CreatePurchaseRequestRequest request) {
        PurchaseRequestId id = createPurchaseRequestUseCase.execute(request.toCommand());
        return ResponseEntity.created(URI.create("/api/purchasing/requests/" + id.value()))
                .body(PurchaseRequestResponse.from(id));
    }

    @PostMapping("/{id}/quotations")
    public ResponseEntity<SendQuotationResponse> sendQuotation(@PathVariable UUID id,
                                                               @Valid @RequestBody SendQuotationRequest request) {
        QuotationId quotationId = sendQuotationUseCase.execute(request.toCommand(id));
        return ResponseEntity.created(URI.create("/api/purchasing/quotations/" + quotationId.value()))
                .body(SendQuotationResponse.from(quotationId));
    }

    @ExceptionHandler(PurchaseRequestNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePurchaseRequestNotFoundException(
            PurchaseRequestNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
