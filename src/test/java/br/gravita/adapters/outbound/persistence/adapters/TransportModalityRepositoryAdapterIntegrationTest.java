package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.TransportModalityDomain;
import br.gravita.core.ports.business.CreateTransportModalityPort;
import br.gravita.core.ports.business.DeleteTransportModalityPort;
import br.gravita.core.ports.business.UpdateTransportModalityPort;
import br.gravita.core.ports.outbound.persistence.TransportModalityRepositoryPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against the real database (Flyway V93): the seeded registry, the soft delete and the one-active-row-per-code rule.
 * Needs PostgreSQL only (SPRING_DATASOURCE_* and the {@code local} profile); the SQS consumers are switched off.
 */
@SpringBootTest(properties = {
		"spring.cloud.aws.sqs.enabled=false",
		"aws.sqs.user-registration-consumer.enabled=false",
		"aws.sqs.password-reset-consumer.enabled=false"})
class TransportModalityRepositoryAdapterIntegrationTest {

	@Autowired
	private TransportModalityRepositoryPort repository;
	@Autowired
	private CreateTransportModalityPort createPort;
	@Autowired
	private UpdateTransportModalityPort updatePort;
	@Autowired
	private DeleteTransportModalityPort deletePort;
	@Autowired
	private JdbcTemplate jdbc;

	private UUID nfeId;
	private UUID createdId;

	@AfterEach
	void cleanUp() {
		if (nfeId != null) {
			jdbc.update("delete from nfe_documents where id = ?", nfeId);
		}
		if (createdId != null) {
			jdbc.update("delete from transport_modalities where id = ?", createdId);
		}
		jdbc.update("update transport_modalities set active = true where code in ('CIF', 'FOB', 'NONE') and name in "
				+ "('CIF — Por conta do emitente', 'FOB — Por conta do destinatário', 'Sem transporte')");
	}

	private TransportModalityDomain activeByCode(final String code) {
		return repository.findActiveByCode(code).orElseThrow();
	}

	@Test
	@DisplayName("The registry is seeded with CIF, FOB and NONE (Sem transporte), all active")
	void shouldBeSeeded() {
		final List<TransportModalityDomain> active = repository.findAllActive();

		assertThat(active).extracting(TransportModalityDomain::getCode).containsExactlyInAnyOrder("CIF", "FOB", "NONE");
		assertThat(active).extracting(TransportModalityDomain::getName)
				.containsExactlyInAnyOrder("CIF — Por conta do emitente", "FOB — Por conta do destinatário", "Sem transporte");
	}

	@Test
	@DisplayName("Delete deactivates the row: it leaves the dropdown list but stays in the full list")
	void shouldSoftDelete() {
		final TransportModalityDomain fob = activeByCode("FOB");

		deletePort.execute(new Context(fob.getId()));

		assertThat(repository.findAllActive()).extracting(TransportModalityDomain::getCode).containsExactlyInAnyOrder("CIF", "NONE");
		assertThat(repository.findAll()).extracting(TransportModalityDomain::getId).contains(fob.getId());
		assertThat(repository.get(fob.getId())).hasValueSatisfying(row -> assertThat(row.getActive()).isFalse());
	}

	@Test
	@DisplayName("A replacement can be created once the active row of that code is deactivated, and the old one cannot be reactivated beside it")
	void shouldReplaceDeactivatedRow() {
		final TransportModalityDomain cif = activeByCode("CIF");
		deletePort.execute(new Context(cif.getId()));

		final TransportModalityDomain replacement = createPort.execute(new Context(
				TransportModalityDomain.builder().name("CIF — frete próprio").code("CIF").build()));
		createdId = replacement.getId();

		assertThat(activeByCode("CIF").getId()).isEqualTo(replacement.getId());
		assertThat(replacement.getCreatedAt()).isNotNull();
		assertThatThrownBy(() -> updatePort.execute(new Context(
				TransportModalityDomain.builder().id(cif.getId()).name(cif.getName()).code("CIF").active(true).build())))
				.hasMessageContaining("already exists");
	}

	@Test
	@DisplayName("The database itself refuses a second active row for the same code")
	void shouldEnforceOneActiveRowPerCode() {
		assertThatThrownBy(() -> jdbc.update("insert into transport_modalities (code, name) values ('FOB', 'duplicada')"))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update("insert into transport_modalities (code, name) values ('XYZ', 'desconhecida')"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("Editing a row keeps its creation timestamp")
	void shouldKeepCreatedAtOnUpdate() {
		final TransportModalityDomain none = activeByCode("NONE");

		final TransportModalityDomain updated = updatePort.execute(new Context(
				TransportModalityDomain.builder().id(none.getId()).name("Sem transporte").code("NONE").build()));

		assertThat(updated.getCreatedAt()).isEqualTo(none.getCreatedAt());
		assertThat(updated.getActive()).isTrue();
	}

	@Test
	@DisplayName("Deactivating the modality an NF-e used leaves the stored document untouched")
	void shouldNotAlterIssuedDocuments() {
		nfeId = UUID.randomUUID();
		jdbc.update("insert into nfe_documents (id, issuer_company_id, natureza_operacao, cfop, recipient_document, recipient_person_type,"
				+ " recipient_name, recipient_state, freight, insurance, other_expenses, status, document_created_at,"
				+ " created_at, modified_at, transport_modality, transport_gross_weight)"
				+ " values (?, ?, 'VENDA', '5102', '12345678000195', 'LEGAL', 'Cliente', 'SP',"
				+ " 10.00, 0, 0, 'AUTHORIZED', now(), now(), now(), 'FOB', 12.5)", nfeId, UUID.randomUUID());
		final Map<String, Object> before = jdbc.queryForMap("select * from nfe_documents where id = ?", nfeId);

		deletePort.execute(new Context(activeByCode("FOB").getId()));

		assertThat(jdbc.queryForMap("select * from nfe_documents where id = ?", nfeId)).isEqualTo(before);
		assertThat(before.get("transport_modality")).isEqualTo("FOB");
	}
}
