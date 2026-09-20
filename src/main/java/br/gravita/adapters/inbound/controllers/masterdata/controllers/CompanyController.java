package br.gravita.adapters.inbound.controllers.masterdata.controllers;

import br.gravita.adapters.inbound.controllers.masterdata.dtos.CompanyResponse;
import br.gravita.adapters.inbound.controllers.masterdata.dtos.RegisterCompanyRequest;
import br.gravita.core.ports.inbound.masterdata.RegisterCompanyUseCase;
import br.gravita.core.domain.masterdata.CompanyId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

	private final RegisterCompanyUseCase registerCompanyUseCase;

	public CompanyController(RegisterCompanyUseCase registerCompanyUseCase) {
		this.registerCompanyUseCase = registerCompanyUseCase;
	}

	@PostMapping
	public ResponseEntity<CompanyResponse> register(@Valid @RequestBody RegisterCompanyRequest request) {
		CompanyId id = registerCompanyUseCase.execute(request.toCommand(null));
		return ResponseEntity.created(URI.create("/api/companies/" + id.value())).body(CompanyResponse.from(id));
	}

	@PatchMapping("/{id}")
	public ResponseEntity<CompanyResponse> update(@PathVariable UUID id, @Valid @RequestBody RegisterCompanyRequest request) {
		CompanyId updatedId = registerCompanyUseCase.execute(request.toCommand(CompanyId.of(id)));
		return ResponseEntity.ok(CompanyResponse.from(updatedId));
	}
}
