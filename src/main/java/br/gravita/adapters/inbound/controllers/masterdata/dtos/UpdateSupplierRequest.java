package br.gravita.adapters.inbound.controllers.masterdata.dtos;

import br.gravita.core.domain.masterdata.PixKey;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.ports.inbound.masterdata.UpdateSupplierCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record UpdateSupplierRequest(
		String document,
		String name,
		List<RegisterSupplierRequest.AddressRequest> addresses,
		List<RegisterSupplierRequest.ContactRequest> contacts,
		@Valid RegisterSupplierRequest.BankAccountRequest bankAccount,
		String pixKey,
		@Positive Integer averageLeadTimeDays,
		String defaultPurchaseCfop,
		String ie,
		String im,
		String cnae,
		Boolean simplesOptante) {

	public UpdateSupplierCommand toCommand(final SupplierId supplierId) {
		return new UpdateSupplierCommand(
				supplierId,
				toDocument(),
				name,
				addresses == null ? null : addresses.stream().map(RegisterSupplierRequest.AddressRequest::toDomain).toList(),
				contacts == null ? null : contacts.stream().map(RegisterSupplierRequest.ContactRequest::toDomain).toList(),
				bankAccount == null ? null : bankAccount.toDomain(),
				pixKey == null || pixKey.isBlank() ? null : PixKey.of(pixKey),
				averageLeadTimeDays,
				defaultPurchaseCfop,
				ie,
				im,
				cnae,
				simplesOptante);
	}

	private Document toDocument() {
		if (document == null || document.isBlank()) {
			return null;
		}
		return Document.cnpj(document);
	}
}
