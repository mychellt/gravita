package br.gravita.adapters.inbound.controllers;

import br.gravita.adapters.dtos.request.InterstateIcmsRateImportRequest;
import br.gravita.adapters.dtos.response.InterstateIcmsRateResponse;
import br.gravita.core.domain.Context;
import br.gravita.core.ports.business.ImportInterstateIcmsRatesPort;
import br.gravita.core.ports.business.ListInterstateIcmsRatesPort;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** No per-row edit endpoint: this table is updated exclusively via import (AC). */
@RestController
@RequestMapping("/api/auxiliary/interstate-icms")
public class InterstateIcmsRateRestController {

	private final ListInterstateIcmsRatesPort listInterstateIcmsRatesPort;
	private final ImportInterstateIcmsRatesPort importInterstateIcmsRatesPort;

	public InterstateIcmsRateRestController(ListInterstateIcmsRatesPort listInterstateIcmsRatesPort,
			ImportInterstateIcmsRatesPort importInterstateIcmsRatesPort) {
		this.listInterstateIcmsRatesPort = listInterstateIcmsRatesPort;
		this.importInterstateIcmsRatesPort = importInterstateIcmsRatesPort;
	}

	@GetMapping
	public ResponseEntity<List<InterstateIcmsRateResponse>> findAll() {
		return ResponseEntity.ok(listInterstateIcmsRatesPort.execute(new Context()).stream().map(InterstateIcmsRateResponse::from).toList());
	}

	@PostMapping("/import")
	public ResponseEntity<List<InterstateIcmsRateResponse>> importRates(@Valid @RequestBody InterstateIcmsRateImportRequest request) {
		List<InterstateIcmsRateResponse> imported = importInterstateIcmsRatesPort.execute(new Context(request.toDomainList()))
				.stream().map(InterstateIcmsRateResponse::from).toList();
		return ResponseEntity.ok(imported);
	}
}
