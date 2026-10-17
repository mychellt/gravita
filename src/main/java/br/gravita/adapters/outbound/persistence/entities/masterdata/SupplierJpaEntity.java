package br.gravita.adapters.outbound.persistence.entities.masterdata;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * A supplier is a company: id, name, document (stored as the company's {@code cnpj}), IE, IM, CNAE and Simples flag live
 * in {@code companies}; this table (joined by id) holds only what is specific to buying from it.
 */
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "suppliers")
@PrimaryKeyJoinColumn(name = "id")
public class SupplierJpaEntity extends CompanyJpaEntity {

    @ElementCollection
    @CollectionTable(name = "supplier_addresses", joinColumns = @JoinColumn(name = "supplier_id"))
    private List<SupplierAddressEmbeddable> addresses;

    @ElementCollection
    @CollectionTable(name = "supplier_contacts", joinColumns = @JoinColumn(name = "supplier_id"))
    private List<SupplierContactEmbeddable> contacts;

    @Column(name = "bank_code", length = 10)
    private String bankCode;

    @Column(name = "bank_agency", length = 20)
    private String bankAgency;
    @Column(name = "bank_account_number", length = 30)
    private String bankAccountNumber;

    @Column(name = "pix_key")
    private String pixKey;

    @Column(name = "average_lead_time_days")
    private Integer averageLeadTimeDays;

    @Column(name = "default_purchase_cfop", length = 4)
    private String defaultPurchaseCfop;
}
