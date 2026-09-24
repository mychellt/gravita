package br.gravita.adapters.inbound.controllers.purchasing.controllers;

import br.gravita.adapters.inbound.controllers.purchasing.dtos.*;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.purchasing.*;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.purchasing.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/purchasing/orders")
public class PurchaseOrderController {

    private final CreatePurchaseOrderUseCase createPurchaseOrderUseCase;
    private final ReceivePurchaseOrderUseCase receivePurchaseOrderUseCase;
    private final ApprovePurchaseOrderUseCase approvePurchaseOrderUseCase;
    private final ImportSupplierNfeAtReceivingUseCase importSupplierNfeAtReceivingUseCase;

    @PostMapping
    public ResponseEntity<PurchaseOrderResponse> create(@Valid @RequestBody CreatePurchaseOrderRequest request) {
        PurchaseOrderId id = createPurchaseOrderUseCase.execute(request.toCommand());
        return ResponseEntity.created(URI.create("/api/purchasing/orders/" + id.value()))
                .body(PurchaseOrderResponse.from(id));
    }

    @PostMapping("/{id}/receipts")
    public ResponseEntity<PurchaseReceiptResponse> receive(@PathVariable UUID id,
                                                           @Valid @RequestBody ReceivePurchaseOrderRequest request) {
        PurchaseReceiptId receiptId = receivePurchaseOrderUseCase.execute(request.toCommand(id));
        return ResponseEntity.created(URI.create("/api/purchasing/receipts/" + receiptId.value()))
                .body(PurchaseReceiptResponse.from(receiptId));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable UUID id,
                                        @Valid @RequestBody ApprovePurchaseOrderRequest request) {
        approvePurchaseOrderUseCase.execute(request.toCommand(id));
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/receipts/{receiptId}/import-nfe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConferenceResultResponse> importNfeAtReceiving(@PathVariable UUID id,
                                                                         @PathVariable UUID receiptId, @RequestParam UUID companyId,
                                                                         @RequestPart("xmlFile") MultipartFile xmlFile) {
        ConferenceResult result = importSupplierNfeAtReceivingUseCase.execute(new ImportSupplierNfeAtReceivingCommand(
                PurchaseOrderId.of(id), PurchaseReceiptId.of(receiptId), CompanyId.of(companyId),
                readBytes(xmlFile)));
        return ResponseEntity.ok(ConferenceResultResponse.from(result));
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read uploaded NFe XML file", e);
        }
    }

    @ExceptionHandler(PurchaseReceiptNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePurchaseReceiptNotFoundException(
            PurchaseReceiptNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(PurchaseRequestNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePurchaseRequestNotFoundException(
            PurchaseRequestNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(PurchaseOrderNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePurchaseOrderNotFoundException(
            PurchaseOrderNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
