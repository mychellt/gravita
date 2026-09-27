package br.gravita.adapters.inbound.controllers.sales.controllers;

import br.gravita.adapters.inbound.controllers.sales.dtos.FunnelConversionResponse;
import br.gravita.core.ports.inbound.sales.GetFunnelConversionQuery;
import br.gravita.core.ports.inbound.sales.GetFunnelConversionUseCase;
import java.time.YearMonth;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/crm/funnel")
public class FunnelConversionController {

	private final GetFunnelConversionUseCase getFunnelConversionUseCase;

	@GetMapping("/conversion")
	public ResponseEntity<FunnelConversionResponse> getConversion(
			@RequestParam(required = false) UUID salesperson,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth period) {
		FunnelConversionResponse response = FunnelConversionResponse
				.from(getFunnelConversionUseCase.execute(new GetFunnelConversionQuery(period, salesperson)));
		return ResponseEntity.ok(response);
	}
}
