package br.gravita.adapters.inbound.controllers.auxiliary;

import br.gravita.adapters.dtos.response.PaymentMethodTypeResponse;
import br.gravita.core.domain.PaymentMethodType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/auxiliary/payment-methods")
public class PaymentMethodRestController {

    @GetMapping
    public ResponseEntity<List<PaymentMethodTypeResponse>> findAll() {
        return ResponseEntity.ok(Arrays.stream(PaymentMethodType.values()).map(PaymentMethodTypeResponse::from).toList());
    }
}
