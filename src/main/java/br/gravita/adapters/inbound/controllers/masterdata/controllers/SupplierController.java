package br.gravita.adapters.inbound.controllers.masterdata.controllers;

import br.gravita.masterdata.adapter.in.web.dto.RegisterSupplierRequest;
import br.gravita.masterdata.adapter.in.web.dto.SupplierResponse;
import br.gravita.core.ports.inbound.masterdata.RegisterSupplierUseCase;
import br.gravita.core.domain.masterdata.SupplierId;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

	private final RegisterSupplierUseCase registerSupplierUseCase;

	public SupplierController(RegisterSupplierUseCase registerSupplierUseCase) {
		this.registerSupplierUseCase = registerSupplierUseCase;
	}

	@PostMapping
	public ResponseEntity<SupplierResponse> register(@Valid @RequestBody RegisterSupplierRequest request) {
		SupplierId id = registerSupplierUseCase.execute(request.toCommand());
		return ResponseEntity.created(URI.create("/api/suppliers/" + id.value())).body(SupplierResponse.from(id));
	}
}
