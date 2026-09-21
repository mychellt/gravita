package br.gravita.adapters.inbound.controllers.masterdata.controllers;

import br.gravita.adapters.inbound.controllers.masterdata.dtos.CompanyResponse;
import br.gravita.adapters.inbound.controllers.masterdata.dtos.RegisterCompanyRequest;
import br.gravita.adapters.inbound.controllers.masterdata.dtos.SwitchSefazEnvironmentRequest;
import br.gravita.core.domain.masterdata.CompanyNotFoundException;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.masterdata.RegisterCompanyUseCase;
import br.gravita.core.ports.inbound.masterdata.SwitchSefazEnvironmentUseCase;
import br.gravita.core.domain.masterdata.CompanyId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

	private final RegisterCompanyUseCase registerCompanyUseCase;
	private final SwitchSefazEnvironmentUseCase switchSefazEnvironmentUseCase;

	public CompanyController(RegisterCompanyUseCase registerCompanyUseCase,
			SwitchSefazEnvironmentUseCase switchSefazEnvironmentUseCase) {
		this.registerCompanyUseCase = registerCompanyUseCase;
		this.switchSefazEnvironmentUseCase = switchSefazEnvironmentUseCase;
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

	@PatchMapping("/{id}/sefaz-environment")
	public ResponseEntity<CompanyResponse> switchSefazEnvironment(@PathVariable UUID id,
			@Valid @RequestBody SwitchSefazEnvironmentRequest request) {
		CompanyId companyId = CompanyId.of(id);
		switchSefazEnvironmentUseCase.execute(request.toCommand(companyId));
		return ResponseEntity.ok(CompanyResponse.from(companyId));
	}

	@ExceptionHandler(CompanyNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleCompanyNotFoundException(CompanyNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}
}
