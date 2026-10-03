package com.example.store;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Testcontainers
class StoreApiIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.2");

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    private long create(String url, Object body) throws Exception {
        String json = mvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    private long createCustomer(String name) throws Exception {
        return create("/customer", Map.of("name", name));
    }

    private long createProduct(String description) throws Exception {
        return create("/products", Map.of("description", description));
    }

    private long createOrder(long customerId, String description, long... productIds) throws Exception {
        List<Map<String, Long>> products = java.util.Arrays.stream(productIds)
                .mapToObj(id -> Map.of("id", id))
                .toList();
        return create(
                "/order",
                Map.of("description", description, "customer", Map.of("id", customerId), "products", products));
    }

    @Test
    void orderIsReturnedWithCustomerAndProducts() throws Exception {
        long customer = createCustomer("Ada Lovelace");
        long p1 = createProduct("Widget");
        long p2 = createProduct("Gadget");
        long order = createOrder(customer, "First order", p1, p2);

        mvc.perform(get("/order/" + order))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("First order"))
                .andExpect(jsonPath("$.customer.id").value(customer))
                .andExpect(jsonPath("$.customer.name").value("Ada Lovelace"))
                .andExpect(jsonPath("$.products.length()").value(2));
    }

    @Test
    void unknownOrderReturns404() throws Exception {
        mvc.perform(get("/order/999999")).andExpect(status().isNotFound());
    }

    @Test
    void orderWithUnknownCustomerReturns404() throws Exception {
        long product = createProduct("Widget");
        mvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "description",
                                "x",
                                "customer",
                                Map.of("id", 999999),
                                "products",
                                List.of(Map.of("id", product))))))
                .andExpect(status().isNotFound());
    }

    @Test
    void orderWithoutProductsReturns400() throws Exception {
        long customer = createCustomer("No Products");
        mvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("description", "x", "customer", Map.of("id", customer), "products", List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankCustomerNameReturns400() throws Exception {
        mvc.perform(post("/customer").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customerSearchMatchesSubstringCaseInsensitively() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        createCustomer("Zebediah " + unique + " Quux");

        mvc.perform(get("/customer").param("name", unique.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Zebediah " + unique + " Quux"));
    }

    @Test
    void productListsTheOrdersContainingIt() throws Exception {
        long customer = createCustomer("Grace Hopper");
        long product = createProduct("Compiler");
        long other = createProduct("Unsold");
        long order1 = createOrder(customer, "A", product);
        long order2 = createOrder(customer, "B", product);

        mvc.perform(get("/products/" + product))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0]").value(order1))
                .andExpect(jsonPath("$.orders[1]").value(order2));

        mvc.perform(get("/products/" + other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders.length()").value(0));

        mvc.perform(get("/products/999999")).andExpect(status().isNotFound());
    }

    @Test
    void getEndpointsDoNotIssueOneQueryPerRow() throws Exception {
        long product = createProduct("Shared product");
        for (int i = 0; i < 5; i++) {
            long customer = createCustomer("Bulk customer " + i);
            createOrder(customer, "Bulk order " + i, product);
        }

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        stats.clear();
        mvc.perform(get("/customer")).andExpect(status().isOk());
        assertThat(stats.getPrepareStatementCount()).as("GET /customer").isLessThanOrEqualTo(2);

        stats.clear();
        mvc.perform(get("/order")).andExpect(status().isOk());
        assertThat(stats.getPrepareStatementCount()).as("GET /order").isLessThanOrEqualTo(2);

        stats.clear();
        mvc.perform(get("/products")).andExpect(status().isOk());
        assertThat(stats.getPrepareStatementCount()).as("GET /products").isLessThanOrEqualTo(2);
    }
}
