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
    public ResponseEntity<PurchaseOrderResponse> create(@Valid @RequestBody final CreatePurchaseOrderRequest request) {
        final PurchaseOrderId id = createPurchaseOrderUseCase.execute(request.toCommand());
        return ResponseEntity.created(URI.create("/api/purchasing/orders/" + id.value()))
                .body(PurchaseOrderResponse.from(id));
    }

    @PostMapping("/{id}/receipts")
    public ResponseEntity<PurchaseReceiptResponse> receive(@PathVariable final UUID id,
                                                           @Valid @RequestBody final ReceivePurchaseOrderRequest request) {
        final PurchaseReceiptId receiptId = receivePurchaseOrderUseCase.execute(request.toCommand(id));
        return ResponseEntity.created(URI.create("/api/purchasing/receipts/" + receiptId.value()))
                .body(PurchaseReceiptResponse.from(receiptId));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable final UUID id,
                                        @Valid @RequestBody final ApprovePurchaseOrderRequest request) {
        approvePurchaseOrderUseCase.execute(request.toCommand(id));
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/receipts/{receiptId}/import-nfe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConferenceResultResponse> importNfeAtReceiving(@PathVariable final UUID id,
                                                                         @PathVariable final UUID receiptId, @RequestParam final UUID companyId,
                                                                         @RequestPart("xmlFile") final MultipartFile xmlFile) {
        final ConferenceResult result = importSupplierNfeAtReceivingUseCase.execute(new ImportSupplierNfeAtReceivingCommand(
                PurchaseOrderId.of(id), PurchaseReceiptId.of(receiptId), CompanyId.of(companyId),
                readBytes(xmlFile)));
        return ResponseEntity.ok(ConferenceResultResponse.from(result));
    }

    private byte[] readBytes(final MultipartFile file) {
        try {
            return file.getBytes();
        } catch (final IOException e) {
            throw new UncheckedIOException("Unable to read uploaded NFe XML file", e);
        }
    }

    @ExceptionHandler(PurchaseReceiptNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePurchaseReceiptNotFoundException(
            final PurchaseReceiptNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(PurchaseRequestNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePurchaseRequestNotFoundException(
            final PurchaseRequestNotFoundException exception) {
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
