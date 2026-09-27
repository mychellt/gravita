package br.gravita.adapters.inbound.controllers.tax.controllers;

import br.gravita.adapters.inbound.controllers.tax.dtos.VoidNumberRangeRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.VoidNumberRangeResponse;
import br.gravita.core.domain.tax.VoidedNumberRange;
import br.gravita.core.ports.inbound.tax.VoidDocumentNumberRangeUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/nfe/void-range")
public class VoidNumberRangeController {

	private final VoidDocumentNumberRangeUseCase voidDocumentNumberRangeUseCase;

	@PostMapping
	public ResponseEntity<VoidNumberRangeResponse> voidRange(@Valid @RequestBody VoidNumberRangeRequest request) {
		VoidedNumberRange voidedNumberRange = voidDocumentNumberRangeUseCase.execute(request.toCommand());
		return ResponseEntity.ok(VoidNumberRangeResponse.from(voidedNumberRange));
	}
}
