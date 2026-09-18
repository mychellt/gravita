package br.gravita.masterdata.adapter.in.web;

import br.gravita.masterdata.adapter.in.web.dto.CompanyResponse;
import br.gravita.masterdata.adapter.in.web.dto.RegisterCompanyRequest;
import br.gravita.masterdata.application.port.in.RegisterCompanyUseCase;
import br.gravita.masterdata.domain.model.CompanyId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
