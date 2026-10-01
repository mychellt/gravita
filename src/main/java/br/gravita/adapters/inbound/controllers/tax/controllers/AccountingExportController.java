package br.gravita.adapters.inbound.controllers.tax.controllers;

import br.gravita.adapters.inbound.controllers.tax.dtos.ExportAccountingEntriesRequest;
import br.gravita.core.ports.inbound.tax.AccountingExportFile;
import br.gravita.core.ports.inbound.tax.ExportAccountingEntriesUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/accounting/export")
public class AccountingExportController {

	private final ExportAccountingEntriesUseCase exportAccountingEntriesUseCase;

	/** Answers with the file itself, as an attachment. */
	@PostMapping
	public ResponseEntity<byte[]> export(@Valid @RequestBody ExportAccountingEntriesRequest request) {
		AccountingExportFile file = exportAccountingEntriesUseCase.execute(request.toCommand());
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(file.fileName()).build().toString())
				.header("X-Entry-Count", String.valueOf(file.entryCount()))
				.contentType(MediaType.parseMediaType(file.contentType() + ";charset=UTF-8")).body(file.content());
	}
}
