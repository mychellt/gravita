package br.gravita.adapters.inbound.controllers;

import br.gravita.adapters.dtos.request.RegisterProductRequest;
import br.gravita.adapters.dtos.response.ProductResponse;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.ports.business.RegisterProductPort;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/products")
public class ProductRestController {

	private final RegisterProductPort registerProductPort;

	public ProductRestController(RegisterProductPort registerProductPort) {
		this.registerProductPort = registerProductPort;
	}

	@PostMapping
	public ResponseEntity<ProductResponse> register(@Valid @RequestBody RegisterProductRequest request) {
		ProductDomain created = registerProductPort.execute(new Context(request.toDomain(null)));
		return ResponseEntity.created(URI.create("/api/products/" + created.getId())).body(ProductResponse.from(created));
	}
}
