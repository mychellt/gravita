package br.gravita.adapters.inbound.controllers;

import br.gravita.adapters.dtos.request.RegisterProductRequest;
import br.gravita.adapters.dtos.request.UpdateProductRequest;
import br.gravita.adapters.dtos.response.ProductResponse;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.ports.business.RegisterProductPort;
import br.gravita.core.ports.business.UpdateProductPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/products")
public class ProductRestController {

    private final RegisterProductPort registerProductPort;
    private final UpdateProductPort updateProductPort;

    @PostMapping
    public ResponseEntity<ProductResponse> register(@Valid @RequestBody RegisterProductRequest request) {
        ProductDomain created = registerProductPort.execute(new Context(request.toDomain(null)));
        return ResponseEntity.created(URI.create("/api/products/" + created.getId())).body(ProductResponse.from(created));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateProductRequest request) {
        ProductDomain updated = updateProductPort.execute(new Context(request.toDomain(id)));
        return ResponseEntity.ok(ProductResponse.from(updated));
    }
}
