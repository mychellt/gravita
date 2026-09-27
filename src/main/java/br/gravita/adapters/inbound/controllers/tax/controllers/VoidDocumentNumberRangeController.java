package br.gravita.adapters.inbound.controllers.tax.controllers;

import br.gravita.adapters.inbound.controllers.tax.dtos.VoidNumberRangeRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.VoidedNumberRangeResponse;
import br.gravita.core.domain.tax.VoidedNumberRange;
import br.gravita.core.ports.inbound.tax.VoidDocumentNumberRangeUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC-M2-06 (module spec's adapter table): registers an `Inutilização` for a
 * range of document numbers that were allocated but never used.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/nfe/void-range")
public class VoidDocumentNumberRangeController {

	private final VoidDocumentNumberRangeUseCase voidDocumentNumberRangeUseCase;

	@PostMapping
	public ResponseEntity<VoidedNumberRangeResponse> voidRange(@Valid @RequestBody VoidNumberRangeRequest request) {
		VoidedNumberRange voidedNumberRange = voidDocumentNumberRangeUseCase.execute(request.toCommand());
		return ResponseEntity.status(HttpStatus.CREATED).body(VoidedNumberRangeResponse.from(voidedNumberRange));
	}
}
