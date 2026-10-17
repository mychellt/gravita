package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.InboundNfeJpaEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Read-only join of the product links with the confirmed inbound NFe lines that carry the linked supplier codes. */
public interface ProductPurchaseHistoryJpaRepository extends JpaRepository<InboundNfeJpaEntity, UUID> {

	interface PurchaseRow {
		UUID getInboundNfeId();

		String getSeries();

		String getNumber();

		String getSupplierDocument();

		String getSupplierName();

		Instant getIssuedAt();

		BigDecimal getQuantity();

		BigDecimal getUnitValue();
	}

	@Query(value = """
			SELECT n.id AS inboundNfeId, n.series AS series, n.number AS number, n.supplier_document AS supplierDocument,
			       n.supplier_name AS supplierName, n.issued_at AS issuedAt, i.quantity AS quantity, i.unit_value AS unitValue
			FROM supplier_product_catalog_links l
			JOIN companies s ON s.id = l.supplier_id
			JOIN inbound_nfes n ON n.company_id = l.company_id AND n.supplier_document = s.cnpj AND n.status = 'CONFIRMED'
			JOIN inbound_nfe_items i ON i.inbound_nfe_id = n.id AND i.supplier_product_code = l.supplier_product_code
			WHERE l.company_id = :companyId AND l.product_id = :productId
			ORDER BY n.issued_at DESC, n.number DESC
			""", nativeQuery = true)
	List<PurchaseRow> findPurchases(@Param("companyId") UUID companyId, @Param("productId") UUID productId);
}
