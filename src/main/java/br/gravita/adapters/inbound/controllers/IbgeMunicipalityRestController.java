package br.gravita.adapters.inbound.controllers;

import br.gravita.adapters.dtos.request.IbgeMunicipalityImportRequest;
import br.gravita.adapters.dtos.response.IbgeMunicipalityResponse;
import br.gravita.core.domain.Context;
import br.gravita.core.ports.business.ImportIbgeMunicipalitiesPort;
import br.gravita.core.ports.business.ListIbgeMunicipalitiesPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auxiliary/ibge-municipalities")
public class IbgeMunicipalityRestController {

    private final ListIbgeMunicipalitiesPort listIbgeMunicipalitiesPort;
    private final ImportIbgeMunicipalitiesPort importIbgeMunicipalitiesPort;

    @GetMapping
    public ResponseEntity<List<IbgeMunicipalityResponse>> findAll() {
        return ResponseEntity.ok(listIbgeMunicipalitiesPort.execute(new Context()).stream().map(IbgeMunicipalityResponse::from).toList());
    }

    @PostMapping("/import")
    public ResponseEntity<List<IbgeMunicipalityResponse>> importMunicipalities(@Valid @RequestBody IbgeMunicipalityImportRequest request) {
        List<IbgeMunicipalityResponse> imported = importIbgeMunicipalitiesPort.execute(new Context(request.toDomainList()))
                .stream().map(IbgeMunicipalityResponse::from).toList();
        return ResponseEntity.ok(imported);
    }
}
