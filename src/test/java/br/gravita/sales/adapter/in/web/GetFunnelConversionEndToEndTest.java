package br.gravita.sales.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.sales.Opportunity;
import br.gravita.core.domain.sales.OpportunityId;
import br.gravita.core.domain.sales.OpportunityStage;
import br.gravita.core.domain.sales.StageTransition;
import br.gravita.core.ports.outbound.persistence.sales.OpportunityRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.StageTransitionRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class GetFunnelConversionEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OpportunityRepositoryPort opportunityRepositoryPort;

	@Autowired
	private StageTransitionRepositoryPort stageTransitionRepositoryPort;

	@Test
	@DisplayName("Reports the conversion rate and volume of the sales funnel for the period")
	void reportsConversionRateAndVolumeForThePeriod() throws Exception {
		UUID salespersonId = UUID.randomUUID();
		OpportunityId opportunityId = persistOpportunity(salespersonId);
		persistTransition(opportunityId, OpportunityStage.PROSPECTING, OpportunityStage.PROPOSAL);
		persistTransition(opportunityId, OpportunityStage.PROPOSAL, OpportunityStage.NEGOTIATION);

		mockMvc.perform(get("/api/crm/funnel/conversion").param("period", YearMonth.now().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.conversionRateByStage.length()").value(2))
				.andExpect(jsonPath("$.volumeBySalesperson['" + salespersonId + "']").value(1));
	}

	@Test
	@DisplayName("Restricts the funnel volume to the given salesperson")
	void filtersVolumeByTheGivenSalesperson() throws Exception {
		UUID salespersonId = UUID.randomUUID();
		UUID otherSalespersonId = UUID.randomUUID();
		OpportunityId ownOpportunity = persistOpportunity(salespersonId);
		OpportunityId otherOpportunity = persistOpportunity(otherSalespersonId);
		persistTransition(ownOpportunity, OpportunityStage.PROSPECTING, OpportunityStage.PROPOSAL);
		persistTransition(otherOpportunity, OpportunityStage.PROSPECTING, OpportunityStage.LOST);

		mockMvc.perform(get("/api/crm/funnel/conversion").param("period", YearMonth.now().toString())
						.param("salesperson", salespersonId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.volumeBySalesperson.length()").value(1))
				.andExpect(jsonPath("$.volumeBySalesperson['" + salespersonId + "']").value(1));
	}

	private OpportunityId persistOpportunity(UUID owner) {
		OpportunityId id = OpportunityId.of(UUID.randomUUID());
		Opportunity opportunity = Opportunity.create(id, UUID.randomUUID(), BigDecimal.valueOf(1000), 50,
				LocalDate.now().plusDays(30), owner);
		return opportunityRepositoryPort.save(opportunity).getId();
	}

	private void persistTransition(OpportunityId opportunityId, OpportunityStage from, OpportunityStage to) {
		stageTransitionRepositoryPort.save(StageTransition.append(opportunityId, from, to));
	}
}
