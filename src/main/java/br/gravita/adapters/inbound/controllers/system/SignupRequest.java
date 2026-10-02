package br.gravita.adapters.inbound.controllers.system;

import br.gravita.core.usercases.system.SignupCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
		@NotBlank @Size(max = 255) String fullName,
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(max = 128) String password,
		@NotBlank @Size(max = 255) String companyName,
		@NotBlank @Size(max = 20) String cnpj,
		@Size(max = 20) String phone,
		@Size(max = 20) String plan,
		@Size(max = 20) String billing) {

	public SignupCommand toCommand() {
		return new SignupCommand(fullName, email, password, companyName, cnpj, phone, plan, billing);
	}

	/** Keeps the plaintext password out of any log line that prints the request. */
	@Override
	public String toString() {
		return "SignupRequest[email=" + email + ", companyName=" + companyName + ", plan=" + plan
				+ ", billing=" + billing + "]";
	}
}
