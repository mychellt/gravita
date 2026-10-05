package br.gravita.adapters.inbound.controllers.purchasing.controllers;

import br.gravita.adapters.inbound.controllers.purchasing.dtos.PurchaseReturnResponse;
import br.gravita.adapters.inbound.controllers.purchasing.dtos.ReturnToSupplierRequest;
import br.gravita.core.domain.purchasing.PurchaseOrderNotFoundException;
import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import br.gravita.core.domain.purchasing.PurchaseReceiptNotFoundException;
import br.gravita.core.domain.purchasing.PurchaseReturnId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.purchasing.ConfirmPurchaseReceiptCommand;
import br.gravita.core.ports.inbound.purchasing.ConfirmPurchaseReceiptUseCase;
import br.gravita.core.ports.inbound.purchasing.ReturnToSupplierUseCase;
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
@RequestMapping("/api/purchasing/receipts")
public class PurchaseReceiptController {

    private final ConfirmPurchaseReceiptUseCase confirmPurchaseReceiptUseCase;
    private final ReturnToSupplierUseCase returnToSupplierUseCase;

    @PostMapping("/{id}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable final UUID id) {
        confirmPurchaseReceiptUseCase.execute(new ConfirmPurchaseReceiptCommand(PurchaseReceiptId.of(id)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<PurchaseReturnResponse> returnToSupplier(@PathVariable final UUID id,
                                                                   @Valid @RequestBody final ReturnToSupplierRequest request) {
        final PurchaseReturnId returnId = returnToSupplierUseCase.execute(request.toCommand(id));
        return ResponseEntity.created(URI.create("/api/purchasing/receipts/" + id + "/return/" + returnId.value()))
                .body(PurchaseReturnResponse.from(returnId));
    }

    @ExceptionHandler(PurchaseReceiptNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePurchaseReceiptNotFoundException(
            final PurchaseReceiptNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(PurchaseOrderNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePurchaseOrderNotFoundException(
            final PurchaseOrderNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, String>> handleBusinessRuleException(final BusinessRuleException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
