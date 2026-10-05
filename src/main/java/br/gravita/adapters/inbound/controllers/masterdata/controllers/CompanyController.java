package br.gravita.adapters.inbound.controllers.masterdata.controllers;

import br.gravita.adapters.inbound.controllers.masterdata.dtos.CompanyResponse;
import br.gravita.adapters.inbound.controllers.masterdata.dtos.ConfigureDocumentSeriesRequest;
import br.gravita.adapters.inbound.controllers.masterdata.dtos.RegisterCompanyRequest;
import br.gravita.adapters.inbound.controllers.masterdata.dtos.SwitchSefazEnvironmentRequest;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.CompanyNotFoundException;
import br.gravita.core.domain.masterdata.DocumentSeriesNotFoundException;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.masterdata.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final RegisterCompanyUseCase registerCompanyUseCase;
    private final SwitchSefazEnvironmentUseCase switchSefazEnvironmentUseCase;
    private final ConfigureDocumentSeriesUseCase configureDocumentSeriesUseCase;
    private final UploadDigitalCertificateUseCase uploadDigitalCertificateUseCase;
    private final GetCompanyUseCase getCompanyUseCase;

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponse> get(@PathVariable final UUID id) {
        return ResponseEntity.ok(companyResponse(CompanyId.of(id)));
    }

    @PostMapping
    public ResponseEntity<CompanyResponse> register(@Valid @RequestBody final RegisterCompanyRequest request) {
        final CompanyId id = registerCompanyUseCase.execute(request.toCommand(null));
        return ResponseEntity.created(URI.create("/api/companies/" + id.value())).body(companyResponse(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CompanyResponse> update(@PathVariable final UUID id, @Valid @RequestBody final RegisterCompanyRequest request) {
        final CompanyId updatedId = registerCompanyUseCase.execute(request.toCommand(CompanyId.of(id)));
        return ResponseEntity.ok(companyResponse(updatedId));
    }

    @PatchMapping("/{id}/sefaz-environment")
    public ResponseEntity<CompanyResponse> switchSefazEnvironment(@PathVariable final UUID id,
                                                                  @Valid @RequestBody final SwitchSefazEnvironmentRequest request) {
        final CompanyId companyId = CompanyId.of(id);
        switchSefazEnvironmentUseCase.execute(request.toCommand(companyId));
        return ResponseEntity.ok(companyResponse(companyId));
    }

    @PutMapping("/{id}/document-series/{type}")
    public ResponseEntity<Void> configureDocumentSeries(@PathVariable final UUID id, @PathVariable("type") final String type,
                                                        @Valid @RequestBody final ConfigureDocumentSeriesRequest request) {
        configureDocumentSeriesUseCase.execute(request.toCommand(CompanyId.of(id), parseDocumentType(type)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/certificate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadCertificate(@PathVariable final UUID id,
                                                  @RequestPart("pfxFile") final MultipartFile pfxFile, @RequestParam final String password,
                                                  @RequestParam(required = false, defaultValue = "A1") final String type) {
        uploadDigitalCertificateUseCase
                .execute(new UploadDigitalCertificateCommand(CompanyId.of(id), type, readBytes(pfxFile), password));
        return ResponseEntity.noContent().build();
    }

    private CompanyResponse companyResponse(final CompanyId id) {
        return CompanyResponse.from(getCompanyUseCase.execute(id));
    }

    private byte[] readBytes(final MultipartFile file) {
        try {
            return file.getBytes();
        } catch (final IOException e) {
            throw new UncheckedIOException("Unable to read uploaded certificate file", e);
        }
    }

    private FiscalDocumentType parseDocumentType(final String type) {
        try {
            return FiscalDocumentType.valueOf(type.toUpperCase());
        } catch (final IllegalArgumentException exception) {
            throw new BusinessRuleException("Unknown document type: " + type);
        }
    }

    @ExceptionHandler(CompanyNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleCompanyNotFoundException(final CompanyNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(DocumentSeriesNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleDocumentSeriesNotFoundException(final DocumentSeriesNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, String>> handleBusinessRuleException(final BusinessRuleException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
