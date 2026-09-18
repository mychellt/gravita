package br.gravita.adapters.inbound.controllers;

import br.gravita.adapters.dtos.request.PaymentMethodRequest;
import br.gravita.adapters.dtos.response.PaymentMethodResponse;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.PaymentMethodDomain;
import br.gravita.core.ports.business.CreatePaymentMethodPort;
import br.gravita.core.ports.business.DeletePaymentMethodPort;
import br.gravita.core.ports.business.FindPaymentMethodPort;
import br.gravita.core.ports.business.ListPaymentMethodsPort;
import br.gravita.core.ports.business.UpdatePaymentMethodPort;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auxiliary/payment-methods")
public class PaymentMethodRestController {

	private final CreatePaymentMethodPort createPaymentMethodPort;
	private final FindPaymentMethodPort findPaymentMethodPort;
	private final ListPaymentMethodsPort listPaymentMethodsPort;
	private final UpdatePaymentMethodPort updatePaymentMethodPort;
	private final DeletePaymentMethodPort deletePaymentMethodPort;

	public PaymentMethodRestController(CreatePaymentMethodPort createPaymentMethodPort, FindPaymentMethodPort findPaymentMethodPort,
			ListPaymentMethodsPort listPaymentMethodsPort, UpdatePaymentMethodPort updatePaymentMethodPort,
			DeletePaymentMethodPort deletePaymentMethodPort) {
		this.createPaymentMethodPort = createPaymentMethodPort;
		this.findPaymentMethodPort = findPaymentMethodPort;
		this.listPaymentMethodsPort = listPaymentMethodsPort;
		this.updatePaymentMethodPort = updatePaymentMethodPort;
		this.deletePaymentMethodPort = deletePaymentMethodPort;
	}

	@PostMapping
	public ResponseEntity<PaymentMethodResponse> create(@Valid @RequestBody PaymentMethodRequest request) {
		PaymentMethodDomain created = createPaymentMethodPort.execute(new Context(request.toDomain(null)));
		return ResponseEntity.created(URI.create("/api/auxiliary/payment-methods/" + created.getId()))
				.body(PaymentMethodResponse.from(created));
	}

	@GetMapping
	public ResponseEntity<List<PaymentMethodResponse>> findAll() {
		return ResponseEntity.ok(listPaymentMethodsPort.execute(new Context()).stream().map(PaymentMethodResponse::from).toList());
	}

	@GetMapping("/{id}")
	public ResponseEntity<PaymentMethodResponse> findById(@PathVariable UUID id) {
		return ResponseEntity.ok(PaymentMethodResponse.from(findPaymentMethodPort.execute(new Context(id))));
	}

	@PatchMapping("/{id}")
	public ResponseEntity<PaymentMethodResponse> update(@PathVariable UUID id, @Valid @RequestBody PaymentMethodRequest request) {
		return ResponseEntity.ok(PaymentMethodResponse.from(updatePaymentMethodPort.execute(new Context(request.toDomain(id)))));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		deletePaymentMethodPort.execute(new Context(id));
		return ResponseEntity.noContent().build();
	}
}
