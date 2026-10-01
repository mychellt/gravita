package br.gravita.purchasing.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.purchasing.PurchaseRequest;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.purchasing.PurchaseRequestItem;
import br.gravita.core.domain.purchasing.PurchaseRequestOrigin;
import br.gravita.core.domain.purchasing.PurchaseRequestStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PurchaseRequestTest {

	@Test
	@DisplayName("Quoting an open request moves it to QUOTED")
	void quotingAnOpenRequestTransitionsItToQuoted() {
		PurchaseRequest request = open();

		PurchaseRequest quoted = request.quote();

		assertThat(quoted.getStatus()).isEqualTo(PurchaseRequestStatus.QUOTED);
		assertThat(quoted.getId()).isEqualTo(request.getId());
		assertThat(quoted.getItems()).isEqualTo(request.getItems());
	}

	@Test
	@DisplayName("Rejects quoting a request that is already quoted")
	void quotingAnAlreadyQuotedRequestIsRejected() {
		PurchaseRequest quoted = PurchaseRequest.of(PurchaseRequestId.of(UUID.randomUUID()),
				PurchaseRequestOrigin.USER, List.of(new PurchaseRequestItem(UUID.randomUUID(), BigDecimal.ONE)),
				UUID.randomUUID(), PurchaseRequestStatus.QUOTED);

		assertThatThrownBy(quoted::quote)
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("OPEN");
	}

	@Test
	@DisplayName("Rejects quoting a request that is already converted")
	void quotingAConvertedRequestIsRejected() {
		PurchaseRequest converted = PurchaseRequest.of(PurchaseRequestId.of(UUID.randomUUID()),
				PurchaseRequestOrigin.USER, List.of(new PurchaseRequestItem(UUID.randomUUID(), BigDecimal.ONE)),
				UUID.randomUUID(), PurchaseRequestStatus.CONVERTED);

		assertThatThrownBy(converted::quote).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Converting an open request moves it to CONVERTED")
	void convertingAnOpenRequestTransitionsItToConverted() {
		PurchaseRequest request = open();

		PurchaseRequest converted = request.convert();

		assertThat(converted.getStatus()).isEqualTo(PurchaseRequestStatus.CONVERTED);
		assertThat(converted.getId()).isEqualTo(request.getId());
	}

	@Test
	@DisplayName("Converting a quoted request moves it to CONVERTED")
	void convertingAQuotedRequestTransitionsItToConverted() {
		PurchaseRequest quoted = PurchaseRequest.of(PurchaseRequestId.of(UUID.randomUUID()),
				PurchaseRequestOrigin.USER, List.of(new PurchaseRequestItem(UUID.randomUUID(), BigDecimal.ONE)),
				UUID.randomUUID(), PurchaseRequestStatus.QUOTED);

		PurchaseRequest converted = quoted.convert();

		assertThat(converted.getStatus()).isEqualTo(PurchaseRequestStatus.CONVERTED);
	}

	@Test
	@DisplayName("Rejects converting a request that is already converted")
	void convertingAnAlreadyConvertedRequestIsRejected() {
		PurchaseRequest alreadyConverted = PurchaseRequest.of(PurchaseRequestId.of(UUID.randomUUID()),
				PurchaseRequestOrigin.USER, List.of(new PurchaseRequestItem(UUID.randomUUID(), BigDecimal.ONE)),
				UUID.randomUUID(), PurchaseRequestStatus.CONVERTED);

		assertThatThrownBy(alreadyConverted::convert)
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("OPEN or QUOTED");
	}

	@Test
	@DisplayName("Rejects converting a cancelled request")
	void convertingACancelledRequestIsRejected() {
		PurchaseRequest cancelled = PurchaseRequest.of(PurchaseRequestId.of(UUID.randomUUID()),
				PurchaseRequestOrigin.USER, List.of(new PurchaseRequestItem(UUID.randomUUID(), BigDecimal.ONE)),
				UUID.randomUUID(), PurchaseRequestStatus.CANCELLED);

		assertThatThrownBy(cancelled::convert).isInstanceOf(BusinessRuleException.class);
	}

	private PurchaseRequest open() {
		return PurchaseRequest.open(PurchaseRequestId.of(UUID.randomUUID()), PurchaseRequestOrigin.USER,
				List.of(new PurchaseRequestItem(UUID.randomUUID(), BigDecimal.TEN)), UUID.randomUUID());
	}
}
