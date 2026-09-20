package br.gravita.masterdata.adapter.in.web;

import br.gravita.masterdata.adapter.in.web.dto.PriceTableResponse;
import br.gravita.masterdata.adapter.in.web.dto.UpsertPriceTableRequest;
import br.gravita.masterdata.application.port.in.ManagePriceTableUseCase;
import br.gravita.masterdata.domain.model.PriceTableId;
import br.gravita.masterdata.domain.model.PriceTableNotFoundException;
import br.gravita.shared.BusinessRuleException;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/price-tables")
public class PriceTableController {

	private final ManagePriceTableUseCase managePriceTableUseCase;

	public PriceTableController(ManagePriceTableUseCase managePriceTableUseCase) {
		this.managePriceTableUseCase = managePriceTableUseCase;
	}

	@PostMapping
	public ResponseEntity<PriceTableResponse> create(@Valid @RequestBody UpsertPriceTableRequest request) {
		PriceTableId id = managePriceTableUseCase.execute(request.toCommand(null));
		return ResponseEntity.created(URI.create("/api/price-tables/" + id.value())).body(PriceTableResponse.from(id));
	}

	@PatchMapping("/{id}")
	public ResponseEntity<PriceTableResponse> update(@PathVariable UUID id,
			@Valid @RequestBody UpsertPriceTableRequest request) {
		PriceTableId saved = managePriceTableUseCase.execute(request.toCommand(id));
		return ResponseEntity.ok(PriceTableResponse.from(saved));
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(PriceTableNotFoundException.class)
	public ResponseEntity<Map<String, String>> handlePriceTableNotFoundException(PriceTableNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}
}
