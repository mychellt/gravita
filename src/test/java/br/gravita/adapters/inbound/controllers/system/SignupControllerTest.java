package br.gravita.adapters.inbound.controllers.system;

import br.gravita.core.domain.BillingCycle;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.PlanTier;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.system.SignupRejectedException;
import br.gravita.core.usercases.system.SignupResult;
import br.gravita.core.usercases.system.SignupUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SignupController.class)
class SignupControllerTest {

    private static final String BODY = """
            {"fullName":"Ana Souza","email":"ana@acme.com","password":"s3cret-pass","companyName":"Acme Ltda",
             "cnpj":"11.222.333/0001-81","phone":"(11) 91234-5678","plan":"silver","billing":"annual"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SignupUseCase signupUseCase;

    @Test
    @DisplayName("Signup answers 201 with the created ids and never echoes the password")
    void shouldCreateAccount() throws Exception {
        final LocalDate today = LocalDate.now();
        when(signupUseCase.execute(any(Context.class))).thenReturn(new SignupResult(UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), PlanTier.SILVER, BillingCycle.ANNUAL, today,
                today.plusDays(365)));

        mockMvc.perform(post("/api/signup").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana@acme.com"))
                .andExpect(jsonPath("$.plan").value("SILVER"))
                .andExpect(jsonPath("$.billingCycle").value("ANNUAL"))
                .andExpect(jsonPath("$.activationDate").value(today.toString()))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("s3cret"))));

        verify(signupUseCase).execute(any(Context.class));
    }

    @Test
    @DisplayName("A rejected signup answers 400 naming the offending field")
    void shouldAnswerBadRequestWithField() throws Exception {
        when(signupUseCase.execute(any(Context.class)))
                .thenThrow(new SignupRejectedException(SignupRejectedException.EMAIL, "Este e-mail já está cadastrado."));

        mockMvc.perform(post("/api/signup").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("email"))
                .andExpect(jsonPath("$.message").value("Este e-mail já está cadastrado."));
    }

    @Test
    @DisplayName("Any other business rule violation answers 400 with its message")
    void shouldAnswerBadRequestForBusinessRule() throws Exception {
        when(signupUseCase.execute(any(Context.class))).thenThrow(new BusinessRuleException("A valid email is required"));

        mockMvc.perform(post("/api/signup").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A valid email is required"));
    }

    @Test
    @DisplayName("A body missing required fields answers 400 without reaching the use case")
    void shouldRejectIncompleteBody() throws Exception {
        mockMvc.perform(post("/api/signup").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"ana@acme.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A unique-constraint race on the CNPJ answers 400 on the cnpj field")
    void shouldMapCnpjConstraintRaceToBadRequest() throws Exception {
        when(signupUseCase.execute(any(Context.class))).thenThrow(new DataIntegrityViolationException("could not execute statement",
                new RuntimeException("ERROR: duplicate key value violates unique constraint \"uq_persons_document\"")));

        mockMvc.perform(post("/api/signup").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("cnpj"));
    }

    @Test
    @DisplayName("A non-unique constraint failure answers 400 without blaming a field")
    void shouldMapOtherConstraintFailureToGenericBadRequest() throws Exception {
        when(signupUseCase.execute(any(Context.class))).thenThrow(new DataIntegrityViolationException("could not execute statement",
                new RuntimeException("ERROR: value too long for type character varying(255)")));

        mockMvc.perform(post("/api/signup").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").doesNotExist());
    }
}
