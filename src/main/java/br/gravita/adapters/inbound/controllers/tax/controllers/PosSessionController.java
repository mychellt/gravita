package br.gravita.adapters.inbound.controllers.tax.controllers;

import br.gravita.adapters.inbound.controllers.tax.dtos.CashClosingReportResponse;
import br.gravita.adapters.inbound.controllers.tax.dtos.ClosePosSessionRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.OpenPosSessionRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.OpenPosSessionResponse;
import br.gravita.core.domain.tax.CashClosingReport;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.ports.inbound.tax.ClosePosSessionUseCase;
import br.gravita.core.ports.inbound.tax.GetZReportUseCase;
import br.gravita.core.ports.inbound.tax.OpenPosSessionUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/pdv/sessions")
public class PosSessionController {

	private final OpenPosSessionUseCase openPosSessionUseCase;
	private final ClosePosSessionUseCase closePosSessionUseCase;
	private final GetZReportUseCase getZReportUseCase;

	@PostMapping
	public ResponseEntity<OpenPosSessionResponse> open(@Valid @RequestBody final OpenPosSessionRequest request) {
		final PosSessionId id = openPosSessionUseCase.execute(request.toCommand());
		return ResponseEntity.created(URI.create("/api/pdv/sessions/" + id.value()))
				.body(OpenPosSessionResponse.from(id));
	}

	@PostMapping("/{id}/close")
	public ResponseEntity<CashClosingReportResponse> close(@PathVariable final UUID id,
			@Valid @RequestBody final ClosePosSessionRequest request) {
		final CashClosingReport report = closePosSessionUseCase.execute(request.toCommand(id));
		return ResponseEntity.ok(CashClosingReportResponse.from(report));
	}

	@GetMapping("/{id}/z-report")
	public ResponseEntity<CashClosingReportResponse> reportZ(@PathVariable final UUID id) {
		final CashClosingReport report = getZReportUseCase.execute(id);
		return ResponseEntity.ok(CashClosingReportResponse.from(report));
	}
}
