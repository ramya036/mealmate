package com.ramya.mealmate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ramya.mealmate.payment.PaymentGateway;
import com.ramya.mealmate.payment.PaymentGateway.Intent;
import com.ramya.mealmate.payment.PaymentGateway.PaymentEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MealMateApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @MockBean
    PaymentGateway payments;

    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        customerToken = register("user-" + UUID.randomUUID() + "@test.dev");
    }

    @Test
    void menuIsPublic() throws Exception {
        mvc.perform(get("/api/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(8)))
                .andExpect(jsonPath("$[0].price").exists());
    }

    @Test
    void ordersRequireAuthentication() throws Exception {
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/orders").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotEditMenuButAdminCan() throws Exception {
        String body = """
                {"name":"Garlic Naan","description":"","price":3.49,"category":"Indian","available":true}""";
        mvc.perform(post("/api/menu").header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/menu").header("Authorization", bearer(login("admin@test.dev", "adminpass123")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Garlic Naan"));
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@test.dev";
        register(email);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\",\"fullName\":\"Dup\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.dev\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fullOrderAndPaymentFlow() throws Exception {
        long pizzaId = menuItemId("Margherita Pizza");
        long lassiId = menuItemId("Mango Lassi");

        // Client-sent prices are ignored: the request has no price field at all.
        JsonNode order = readJson(mvc.perform(auth(post("/api/orders"), customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryAddress\":\"33 Gilmer St, Atlanta\",\"items\":[" +
                                "{\"menuItemId\":" + pizzaId + ",\"quantity\":2}," +
                                "{\"menuItemId\":" + lassiId + ",\"quantity\":1}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andReturn().getResponse().getContentAsString());
        long orderId = order.get("id").asLong();
        assertThat(order.get("total").decimalValue()).isEqualByComparingTo("30.47");

        when(payments.createIntent(eq(3047L), eq("usd"), eq(orderId), anyString()))
                .thenReturn(new Intent("pi_test_123", "pi_test_123_secret"));
        mvc.perform(auth(post("/api/orders/" + orderId + "/checkout"), customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientSecret").value("pi_test_123_secret"))
                .andExpect(jsonPath("$.amountCents").value(3047));

        when(payments.parseWebhook(anyString(), eq("valid-sig")))
                .thenReturn(Optional.of(new PaymentEvent("payment_intent.succeeded", "pi_test_123")));
        mvc.perform(post("/api/payments/webhook").header("Stripe-Signature", "valid-sig").content("{}"))
                .andExpect(status().isOk());
        // Replayed webhook is harmless.
        mvc.perform(post("/api/payments/webhook").header("Stripe-Signature", "valid-sig").content("{}"))
                .andExpect(status().isOk());

        mvc.perform(auth(get("/api/orders/" + orderId), customerToken))
                .andExpect(jsonPath("$.status").value("PAID"));

        // Customer can't advance status; admin can, but only along valid transitions.
        String admin = login("admin@test.dev", "adminpass123");
        mvc.perform(auth(patch("/api/admin/orders/" + orderId + "/status"), customerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PREPARING\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(auth(patch("/api/admin/orders/" + orderId + "/status"), admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isConflict());
        mvc.perform(auth(patch("/api/admin/orders/" + orderId + "/status"), admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PREPARING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PREPARING"));
    }

    @Test
    void customersCannotSeeEachOthersOrders() throws Exception {
        long pizzaId = menuItemId("Margherita Pizza");
        JsonNode order = readJson(mvc.perform(auth(post("/api/orders"), customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryAddress\":\"Somewhere\",\"items\":[{\"menuItemId\":" + pizzaId + ",\"quantity\":1}]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());

        String other = register("other-" + UUID.randomUUID() + "@test.dev");
        mvc.perform(auth(get("/api/orders/" + order.get("id").asLong()), other))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidWebhookSignatureIsRejected() throws Exception {
        when(payments.parseWebhook(anyString(), any())).thenReturn(Optional.empty());
        mvc.perform(post("/api/payments/webhook").header("Stripe-Signature", "forged").content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void orderValidation() throws Exception {
        mvc.perform(auth(post("/api/orders"), customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryAddress\":\"\",\"items\":[]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(post("/api/orders"), customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryAddress\":\"x\",\"items\":[{\"menuItemId\":999999,\"quantity\":1}]}"))
                .andExpect(status().isBadRequest());
    }

    // --- helpers ---

    private String register(String email) throws Exception {
        String res = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\",\"fullName\":\"Test User\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return readJson(res).get("token").asText();
    }

    private String login(String email, String password) throws Exception {
        String res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return readJson(res).get("token").asText();
    }

    private long menuItemId(String name) throws Exception {
        JsonNode items = readJson(mvc.perform(get("/api/menu")).andReturn().getResponse().getContentAsString());
        for (JsonNode n : items) {
            if (n.get("name").asText().equals(name)) {
                return n.get("id").asLong();
            }
        }
        throw new AssertionError("Menu item not found: " + name);
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req, String token) {
        return req.header("Authorization", bearer(token));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private JsonNode readJson(String s) throws Exception {
        return json.readTree(s);
    }
}
