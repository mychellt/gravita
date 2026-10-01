package br.gravita.finance.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.PayableStatus;
import br.gravita.core.domain.system.ApprovalAlcada;
import br.gravita.core.domain.system.ApprovalModule;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.User;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.ApprovalAlcadaRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ApprovePayableEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PayableRepositoryPort payableRepositoryPort;

	@Autowired
	private ApprovalAlcadaRepositoryPort approvalAlcadaRepositoryPort;

	@Autowired
	private UserRepositoryPort userRepositoryPort;

	private Payable persistOpenPayable(String amount) {
		return payableRepositoryPort.save(Payable.createManual(PayableId.of(UUID.randomUUID()), null,
				new BigDecimal(amount), LocalDate.now().plusDays(7), null));
	}

	private ResultActions approve(UUID payableId, UUID approvedBy) throws Exception {
		return mockMvc.perform(post("/api/finance/payables/" + payableId + "/approve")
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"approvedBy": "%s"}
						""".formatted(approvedBy)));
	}

	private void configureAlcada(String threshold, UUID elevatedProfileId) {
		approvalAlcadaRepositoryPort.save(ApprovalAlcada.configure(ApprovalModule.FINANCE, new BigDecimal(threshold),
				null, new ProfileReference(elevatedProfileId, "Finance Manager")));
	}

	private UUID persistUser(UUID profileId) {
		String email = "approver-" + UUID.randomUUID() + "@example.com";
		return userRepositoryPort
				.save(User.register("Approver", email, "s3cret!", new ProfileReference(profileId, "Profile")))
				.getId().value();
	}

	@Test
	@DisplayName("Approves an open payable over HTTP and persists the approver on it")
	void approvesAnOpenPayableAndPersistsTheApprover() throws Exception {
		Payable payable = persistOpenPayable("75.00");
		UUID approver = UUID.randomUUID();

		approve(payable.getId().value(), approver).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(payable.getId().value().toString()))
				.andExpect(jsonPath("$.status").value("APPROVED"))
				.andExpect(jsonPath("$.approvedBy").value(approver.toString()));

		Payable reloaded = payableRepositoryPort.findById(payable.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(PayableStatus.APPROVED);
		assertThat(reloaded.getApprovedBy()).isEqualTo(approver);
	}

	@Test
	@DisplayName("Approves a payable above the approval limit when the approver has the elevated profile")
	void approvesAPayableExceedingTheAlcadaWhenTheApproverHasTheElevatedProfile() throws Exception {
		UUID elevatedProfile = UUID.randomUUID();
		configureAlcada("100.00", elevatedProfile);
		UUID approver = persistUser(elevatedProfile);
		Payable payable = persistOpenPayable("500.00");

		approve(payable.getId().value(), approver).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPROVED"))
				.andExpect(jsonPath("$.approvedBy").value(approver.toString()));
	}

	@Test
	@DisplayName("Rejects approval of a payable above the approval limit when the approver lacks the elevated profile")
	void rejectsAPayableExceedingTheAlcadaWhenTheApproverLacksTheElevatedProfile() throws Exception {
		configureAlcada("100.00", UUID.randomUUID());
		UUID approver = persistUser(UUID.randomUUID());
		Payable payable = persistOpenPayable("500.00");

		approve(payable.getId().value(), approver).andExpect(status().isBadRequest());

		Payable reloaded = payableRepositoryPort.findById(payable.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(PayableStatus.OPEN);
		assertThat(reloaded.getApprovedBy()).isNull();
	}

	@Test
	@DisplayName("Rejects approval of a payable above the approval limit when the approver is unknown")
	void rejectsAnUnknownApproverWhenThePayableExceedsTheAlcada() throws Exception {
		configureAlcada("100.00", UUID.randomUUID());
		Payable payable = persistOpenPayable("500.00");

		approve(payable.getId().value(), UUID.randomUUID()).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Rejects approving a payable that is already approved")
	void rejectsApprovingAPayableThatIsAlreadyApproved() throws Exception {
		Payable payable = persistOpenPayable("75.00");
		approve(payable.getId().value(), UUID.randomUUID()).andExpect(status().isOk());

		approve(payable.getId().value(), UUID.randomUUID()).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Responds 404 Not Found when the payable to approve does not exist")
	void aPayableThatDoesNotExistIsRejectedWith404() throws Exception {
		approve(UUID.randomUUID(), UUID.randomUUID()).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Rejects an approval request that does not name an approver")
	void aRequestWithoutAnApproverIsRejected() throws Exception {
		Payable payable = persistOpenPayable("75.00");

		mockMvc.perform(post("/api/finance/payables/" + payable.getId().value() + "/approve")
				.contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
	}
}
