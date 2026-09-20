package br.gravita.adapters.outbound.persistence.mappers.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.SupplierAddressEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.masterdata.SupplierContactEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.masterdata.SupplierJpaEntity;
import br.gravita.core.domain.masterdata.Address;
import br.gravita.core.domain.masterdata.BankAccount;
import br.gravita.core.domain.masterdata.Contact;
import br.gravita.core.domain.masterdata.PixKey;
import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonType;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface SupplierPersistenceMapper {

    default Supplier toDomain(final SupplierJpaEntity entity) {
        Document document = entity.getPersonType() == PersonType.INDIVIDUAL
                ? Document.cpf(entity.getDocument())
                : Document.cnpj(entity.getDocument());

        return Supplier.of(
                SupplierId.of(entity.getId()),
                document,
                entity.getName(),
                toAddresses(entity.getAddresses()),
                toContacts(entity.getContacts()),
                toBankAccount(entity),
                entity.getPixKey() == null ? null : PixKey.of(entity.getPixKey()),
                entity.getAverageLeadTimeDays(),
                entity.getDefaultPurchaseCfop());
    }

    default SupplierJpaEntity toEntity(final Supplier domain) {
        BankAccount bankAccount = domain.getBankAccount();
        PixKey pixKey = domain.getPixKey();

        return SupplierJpaEntity.builder()
                .id(domain.getId() == null ? null : domain.getId().value())
                .document(domain.getDocument().number())
                .personType(domain.getDocument().personType())
                .name(domain.getName())
                .addresses(toAddressEmbeddables(domain.getAddresses()))
                .contacts(toContactEmbeddables(domain.getContacts()))
                .bankCode(bankAccount == null ? null : bankAccount.bankCode())
                .bankAgency(bankAccount == null ? null : bankAccount.agency())
                .bankAccountNumber(bankAccount == null ? null : bankAccount.accountNumber())
                .pixKey(pixKey == null ? null : pixKey.value())
                .averageLeadTimeDays(domain.getAverageLeadTimeDays())
                .defaultPurchaseCfop(domain.getDefaultPurchaseCfop())
                .build();
    }

    private BankAccount toBankAccount(final SupplierJpaEntity entity) {
        if (entity.getBankCode() == null) {
            return null;
        }
        return new BankAccount(entity.getBankCode(), entity.getBankAgency(), entity.getBankAccountNumber());
    }

    private List<Address> toAddresses(final List<SupplierAddressEmbeddable> embeddables) {
        if (embeddables == null) {
            return List.of();
        }
        return embeddables.stream()
                .map(e -> new Address(e.getStreet(), e.getNumber(), e.getComplement(), e.getNeighborhood(),
                        e.getCity(), e.getState(), e.getZipCode()))
                .toList();
    }

    private List<Contact> toContacts(final List<SupplierContactEmbeddable> embeddables) {
        if (embeddables == null) {
            return List.of();
        }
        return embeddables.stream().map(e -> new Contact(e.getType(), e.getValue())).toList();
    }

    private List<SupplierAddressEmbeddable> toAddressEmbeddables(final List<Address> addresses) {
        // Hibernate merges a detached entity's collections in place (clear + addAll), so
        // this must stay mutable rather than an immutable Stream.toList().
        return addresses.stream()
                .map(address -> SupplierAddressEmbeddable.builder()
                        .street(address.street())
                        .number(address.number())
                        .complement(address.complement())
                        .neighborhood(address.neighborhood())
                        .city(address.city())
                        .state(address.state())
                        .zipCode(address.zipCode())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<SupplierContactEmbeddable> toContactEmbeddables(final List<Contact> contacts) {
        return contacts.stream()
                .map(contact -> SupplierContactEmbeddable.builder().type(contact.type()).value(contact.value()).build())
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
