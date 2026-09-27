package br.gravita.adapters.inbound.controllers.tax.dtos;

import jakarta.validation.constraints.NotBlank;

public record IssueCorrectionLetterRequest(@NotBlank String text) {
}
