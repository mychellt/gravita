package br.gravita.adapters.inbound.controllers.auxiliary;

import br.gravita.adapters.dtos.request.PaymentTermRequest;
import br.gravita.adapters.dtos.response.PaymentTermResponse;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.PaymentTermDomain;
import br.gravita.core.ports.business.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auxiliary/payment-terms")
public class PaymentTermRestController {

    private final CreatePaymentTermPort createPaymentTermPort;
    private final FindPaymentTermPort findPaymentTermPort;
    private final ListPaymentTermsPort listPaymentTermsPort;
    private final UpdatePaymentTermPort updatePaymentTermPort;
    private final DeletePaymentTermPort deletePaymentTermPort;

    @PostMapping
    public ResponseEntity<PaymentTermResponse> create(@Valid @RequestBody final PaymentTermRequest request) {
        final PaymentTermDomain created = createPaymentTermPort.execute(new Context(request.toDomain(null)));
        return ResponseEntity.created(URI.create("/api/auxiliary/payment-terms/" + created.getId()))
                .body(PaymentTermResponse.from(created));
    }

    @GetMapping
    public ResponseEntity<List<PaymentTermResponse>> findAll() {
        return ResponseEntity.ok(listPaymentTermsPort.execute(new Context()).stream().map(PaymentTermResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentTermResponse> findById(@PathVariable final UUID id) {
        return ResponseEntity.ok(PaymentTermResponse.from(findPaymentTermPort.execute(new Context(id))));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PaymentTermResponse> update(@PathVariable final UUID id, @Valid @RequestBody final PaymentTermRequest request) {
        return ResponseEntity.ok(PaymentTermResponse.from(updatePaymentTermPort.execute(new Context(request.toDomain(id)))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable final UUID id) {
        deletePaymentTermPort.execute(new Context(id));
        return ResponseEntity.noContent().build();
    }
}
