package br.gravita.adapters.inbound.controllers.masterdata.dtos;

import br.gravita.core.domain.masterdata.*;
import br.gravita.core.ports.inbound.masterdata.RegisterSupplierCommand;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record RegisterSupplierRequest(
		@NotNull PersonType personType,
		@NotBlank String document,
		@NotBlank String name,
		@NotEmpty List<@Valid AddressRequest> addresses,
		List<@Valid ContactRequest> contacts,
		@Valid BankAccountRequest bankAccount,
		String pixKey,
		@Positive Integer averageLeadTimeDays,
		String defaultPurchaseCfop) {

	public RegisterSupplierCommand toCommand() {
		final Document supplierDocument = personType == PersonType.INDIVIDUAL ? Document.cpf(document) : Document.cnpj(document);
		return new RegisterSupplierCommand(
				supplierDocument,
				name,
				addresses.stream().map(AddressRequest::toDomain).toList(),
				contacts == null ? List.of() : contacts.stream().map(ContactRequest::toDomain).toList(),
				bankAccount == null ? null : bankAccount.toDomain(),
				pixKey == null || pixKey.isBlank() ? null : PixKey.of(pixKey),
				averageLeadTimeDays,
				defaultPurchaseCfop);
	}

	public record AddressRequest(
			@NotBlank String street,
			String number,
			String complement,
			@NotBlank String neighborhood,
			@NotBlank String city,
			@NotBlank String state,
			@NotBlank String zipCode) {

		Address toDomain() {
			return new Address(street, number, complement, neighborhood, city, state, zipCode);
		}
	}

	public record ContactRequest(@NotNull ContactType type, @NotBlank String value) {

		Contact toDomain() {
			return new Contact(type, value);
		}
	}

	public record BankAccountRequest(@NotBlank String bankCode, @NotBlank String agency, @NotBlank String accountNumber) {

		BankAccount toDomain() {
			return new BankAccount(bankCode, agency, accountNumber);
		}
	}
}
