package br.gravita.adapters.inbound.controllers.auxiliary;

import br.gravita.adapters.dtos.request.InterstateIcmsRateImportRequest;
import br.gravita.adapters.dtos.response.InterstateIcmsRateResponse;
import br.gravita.core.domain.Context;
import br.gravita.core.ports.business.ImportInterstateIcmsRatesPort;
import br.gravita.core.ports.business.ListInterstateIcmsRatesPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auxiliary/interstate-icms")
public class InterstateIcmsRateRestController {

    private final ListInterstateIcmsRatesPort listInterstateIcmsRatesPort;
    private final ImportInterstateIcmsRatesPort importInterstateIcmsRatesPort;

    @GetMapping
    public ResponseEntity<List<InterstateIcmsRateResponse>> findAll() {
        return ResponseEntity.ok(listInterstateIcmsRatesPort.execute(new Context()).stream().map(InterstateIcmsRateResponse::from).toList());
    }

    @PostMapping("/import")
    public ResponseEntity<List<InterstateIcmsRateResponse>> importRates(@Valid @RequestBody final InterstateIcmsRateImportRequest request) {
        final List<InterstateIcmsRateResponse> imported = importInterstateIcmsRatesPort.execute(new Context(request.toDomainList()))
                .stream().map(InterstateIcmsRateResponse::from).toList();
        return ResponseEntity.ok(imported);
    }
}
