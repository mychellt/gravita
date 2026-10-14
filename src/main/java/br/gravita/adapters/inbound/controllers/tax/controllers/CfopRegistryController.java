package br.gravita.adapters.inbound.controllers.tax.controllers;

import br.gravita.adapters.inbound.controllers.tax.dtos.CfopRegistryEntryResponse;
import br.gravita.core.domain.Context;
import br.gravita.core.ports.inbound.tax.ListCfopRegistryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auxiliary/cfop-registry")
public class CfopRegistryController {

    private final ListCfopRegistryUseCase listCfopRegistryUseCase;

    @GetMapping
    public List<CfopRegistryEntryResponse> findAll() {
        return listCfopRegistryUseCase.execute(new Context()).stream().map(CfopRegistryEntryResponse::from).toList();
    }
}
