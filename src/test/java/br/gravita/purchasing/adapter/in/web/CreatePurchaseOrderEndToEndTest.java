package br.gravita.purchasing.adapter.in.web;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.purchasing.Quotation;
import br.gravita.core.domain.purchasing.QuotationId;
import br.gravita.core.domain.purchasing.QuotationItem;
import br.gravita.core.domain.purchasing.QuotationItemPrice;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.QuotationRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class CreatePurchaseOrderEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuotationRepositoryPort quotationRepositoryPort;

    @Autowired
    private PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;

    @Test
    void anOrderCanBeCreatedFromAnOpenRequestWithoutAFormalQuotation() throws Exception {
        UUID requestId = createOpenRequest();

        String body = """
                {
                  "requestId": "%s",
                  "supplierId": "%s",
                  "items": [
                    {"productId": "%s", "quantity": 10, "unitPrice": 2.50}
                  ]
                }
                """.formatted(requestId, UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/purchasing/orders").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void anOrderCreatedFromAQuotationPricesItemsFromTheSelectedSuppliersResponse() throws Exception {
        UUID requestId = createOpenRequest();
        UUID quotationId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Quotation quotation = Quotation.send(QuotationId.of(quotationId), PurchaseRequestId.of(requestId),
                        List.of(new QuotationItem(productId, BigDecimal.ONE)), List.of(SupplierId.of(supplierId)))
                .registerResponse(SupplierId.of(supplierId),
                        List.of(new QuotationItemPrice(productId, new BigDecimal("9.99"))),
                        LocalDate.now().plusDays(5));
        quotationRepositoryPort.save(quotation);

        String body = """
                {
                  "requestId": "%s",
                  "quotationId": "%s",
                  "supplierId": "%s",
                  "items": [{"productId": "%s", "quantity": 1, "unitPrice": 1}]
                }
                """.formatted(requestId, quotationId, supplierId, UUID.randomUUID());

        String response = mockMvc.perform(post("/api/purchasing/orders").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();

        UUID orderId = UUID.fromString(objectMapper.readTree(response).get("id").asString());
        var savedOrder = purchaseOrderRepositoryPort.findById(PurchaseOrderId.of(orderId)).orElseThrow();
        assertThat(savedOrder.getItems()).extracting(PurchaseOrderItem::productId, PurchaseOrderItem::unitPrice)
                .containsExactly(tuple(productId, new BigDecimal("9.99")));
    }

    @Test
    void creatingAnOrderFromAnUnknownRequestIsRejectedWith404() throws Exception {
        String body = """
                {
                  "requestId": "%s",
                  "supplierId": "%s",
                  "items": [{"productId": "%s", "quantity": 1, "unitPrice": 1}]
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/purchasing/orders").contentType("application/json").content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void anEmptyItemListIsRejectedWith400() throws Exception {
        UUID requestId = createOpenRequest();

        String body = """
                {
                  "requestId": "%s",
                  "supplierId": "%s",
                  "items": []
                }
                """.formatted(requestId, UUID.randomUUID());

        mockMvc.perform(post("/api/purchasing/orders").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void convertingTheSameRequestTwiceIsRejectedWith400OnTheSecondAttempt() throws Exception {
        UUID requestId = createOpenRequest();
        String body = """
                {
                  "requestId": "%s",
                  "supplierId": "%s",
                  "items": [{"productId": "%s", "quantity": 1, "unitPrice": 1}]
                }
                """.formatted(requestId, UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/purchasing/orders").contentType("application/json").content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/purchasing/orders").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    private UUID createOpenRequest() throws Exception {
        String body = """
                {
                  "origin": "USER",
                  "requestedBy": "%s",
                  "items": [{"productId": "%s", "quantity": 10}]
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        String response = mockMvc.perform(post("/api/purchasing/requests").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return UUID.fromString(objectMapper.readTree(response).get("id").asString());
    }
}
