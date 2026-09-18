package br.gravita.adapters.dtos.request;

import br.gravita.core.domain.IbgeMunicipalityDomain;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record IbgeMunicipalityImportRequest(@NotEmpty List<@Valid Row> municipalities) {

	public record Row(
			@NotBlank @Pattern(regexp = "\\d{7}") String ibgeCode,
			@NotBlank String name,
			@NotBlank @Pattern(regexp = "[A-Z]{2}") String stateCode) {
	}

	public List<IbgeMunicipalityDomain> toDomainList() {
		return municipalities.stream()
				.map(row -> IbgeMunicipalityDomain.builder()
						.ibgeCode(row.ibgeCode())
						.name(row.name())
						.stateCode(row.stateCode())
						.build())
				.toList();
	}
}
