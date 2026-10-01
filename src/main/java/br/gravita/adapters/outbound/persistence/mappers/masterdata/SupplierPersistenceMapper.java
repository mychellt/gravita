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
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;

import java.util.UUID;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface SupplierPersistenceMapper {

    @Mapping(target = "document", source = "entity", qualifiedByName = "toDocument")
    @Mapping(target = "bankAccount", source = "entity", qualifiedByName = "toBankAccount",
            conditionExpression = "java(entity.getBankCode() != null)")
    @Mapping(target = "pixKey", source = "pixKey", qualifiedByName = "toPixKey")
    Supplier map(final SupplierJpaEntity entity);

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "document", source = "document.number")
    @Mapping(target = "personType", source = "document.personType")
    @Mapping(target = "bankCode", source = "bankAccount.bankCode")
    @Mapping(target = "bankAgency", source = "bankAccount.agency")
    @Mapping(target = "bankAccountNumber", source = "bankAccount.accountNumber")
    @Mapping(target = "pixKey", source = "pixKey.value")
    SupplierJpaEntity map(final Supplier domain);

    Address map(final SupplierAddressEmbeddable embeddable);

    SupplierAddressEmbeddable map(final Address domain);

    Contact map(final SupplierContactEmbeddable embeddable);

    SupplierContactEmbeddable map(final Contact domain);

    @Mapping(target = "value", source = "id")
    SupplierId map(final UUID id);

    @Named("toBankAccount")
    @Mapping(target = "agency", source = "bankAgency")
    @Mapping(target = "accountNumber", source = "bankAccountNumber")
    BankAccount toBankAccount(final SupplierJpaEntity entity);

    @Named("toDocument")
    static Document toDocument(final SupplierJpaEntity entity) {
        return entity.getPersonType() == PersonType.INDIVIDUAL
                ? Document.cpf(entity.getDocument())
                : Document.cnpj(entity.getDocument());
    }

    @Named("toPixKey")
    static PixKey toPixKey(final String pixKey) {
        return PixKey.of(pixKey);
    }
}
