package br.gravita.adapters.inbound.controllers.system;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record SignupRequest(
        @NotBlank @Size(max = 255) String fullName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 128) String password,
        @NotBlank @Size(max = 255) String companyName,
        @NotBlank @Size(max = 20) String cnpj,
        @Size(max = 20) String phone,
        @Size(max = 20) String plan,
        @Size(max = 20) String billing) {
}
