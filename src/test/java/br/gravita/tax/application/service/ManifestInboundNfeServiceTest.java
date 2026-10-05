package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.InboundManifestation;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeStatus;
import br.gravita.core.domain.tax.InboundNfeTotals;
import br.gravita.core.domain.tax.ManifestationType;
import br.gravita.core.ports.inbound.tax.ManifestInboundNfeCommand;
import br.gravita.core.ports.outbound.persistence.tax.InboundManifestationRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.ports.outbound.tax.SefazManifestationRequest;
import br.gravita.core.ports.outbound.tax.SefazSubmissionResult;
import br.gravita.core.ports.outbound.tax.SubmitToSefazPort;
import br.gravita.core.usercases.tax.ManifestInboundNfeService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManifestInboundNfeServiceTest {

	private static final String ACCESS_KEY = "35260112345678000199550010000001231000001234";

	@Mock
	private InboundNfeRepositoryPort inboundNfeRepositoryPort;

	@Mock
	private SubmitToSefazPort submitToSefazPort;

	@Mock
	private InboundManifestationRepositoryPort inboundManifestationRepositoryPort;

	private ManifestInboundNfeService service;

	@BeforeEach
	void setUp() {
		service = new ManifestInboundNfeService(inboundNfeRepositoryPort, submitToSefazPort,
				inboundManifestationRepositoryPort);

		lenient().when(inboundNfeRepositoryPort.findByAccessKey(ACCESS_KEY)).thenReturn(Optional.empty());
		lenient().when(submitToSefazPort.manifest(any())).thenReturn(new SefazSubmissionResult("manifest-protocol-1"));
		lenient().when(inboundManifestationRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	@DisplayName("Offers exactly the three manifestation types from the spec and no others")
	void ac1ExactlyTheThreeManifestationTypesFromTheSpecExistNoOthers() {
		assertThat(ManifestationType.values()).containsExactlyInAnyOrder(ManifestationType.CONFIRMED,
				ManifestationType.UNKNOWN, ManifestationType.OPERATION_NOT_PERFORMED);
	}

	@Test
	@DisplayName("Records the manifestation with a timestamp and forwards it to SEFAZ")
	void ac2TheManifestationIsRecordedWithATimestampAndForwardedToSefaz() {
		final InboundManifestation result = service.execute(new ManifestInboundNfeCommand(ACCESS_KEY, ManifestationType.CONFIRMED));

		final ArgumentCaptor<SefazManifestationRequest> captor = ArgumentCaptor.forClass(SefazManifestationRequest.class);
		verify(submitToSefazPort).manifest(captor.capture());
		assertThat(captor.getValue().accessKey()).isEqualTo(ACCESS_KEY);
		assertThat(captor.getValue().type()).isEqualTo(ManifestationType.CONFIRMED);

		assertThat(result.getAccessKey()).isEqualTo(ACCESS_KEY);
		assertThat(result.getType()).isEqualTo(ManifestationType.CONFIRMED);
		assertThat(result.getSefazProtocol()).isEqualTo("manifest-protocol-1");
		assertThat(result.getManifestedAt()).isNotNull();

		verify(inboundManifestationRepositoryPort).save(result);
	}

	@Test
	@DisplayName("Works by access key alone even when no matching inbound NF-e exists yet")
	void ac3WorksByAccessKeyAloneEvenWhenNoMatchingInboundNfeRowExistsYet() {
		final InboundManifestation result = service
				.execute(new ManifestInboundNfeCommand(ACCESS_KEY, ManifestationType.UNKNOWN));

		assertThat(result.getInboundNfeId()).isEmpty();
		verify(inboundManifestationRepositoryPort).save(any());
	}

	@Test
	@DisplayName("Links the manifestation back to the matching inbound NF-e when one exists")
	void whenAMatchingInboundNfeExistsTheManifestationLinksBackToIt() {
		final InboundNfe inboundNfe = existingInboundNfe();
		when(inboundNfeRepositoryPort.findByAccessKey(ACCESS_KEY)).thenReturn(Optional.of(inboundNfe));

		final InboundManifestation result = service
				.execute(new ManifestInboundNfeCommand(ACCESS_KEY, ManifestationType.OPERATION_NOT_PERFORMED));

		assertThat(result.getInboundNfeId()).contains(inboundNfe.getId());
	}

	private InboundNfe existingInboundNfe() {
		return InboundNfe.builder()
				.id(InboundNfeId.of(UUID.randomUUID()))
				.companyId(CompanyId.of(UUID.randomUUID()))
				.accessKey(ACCESS_KEY)
				.series("001")
				.number("123")
				.supplierDocument(Document.cnpj("11.222.333/0001-81"))
				.supplierName("Supplier Ltda")
				.issuedAt(Instant.parse("2026-01-10T12:00:00Z"))
				.items(List.of(new InboundNfeItem("SKU-1", "Product", "12341234", "5102", "UN", BigDecimal.ONE,
						BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
						BigDecimal.ZERO)))
				.totals(new InboundNfeTotals(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
						BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
						BigDecimal.TEN))
				.xmlStorageRef("MANUAL_ENTRY")
				.status(InboundNfeStatus.PENDING_CONFERENCE)
				.importedAt(Instant.now())
				.build();
	}
}
