package com.example.store.controller;

import com.example.store.dto.CreateOrderRequest;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.entity.Product;
import com.example.store.mapper.CustomerMapper;
import com.example.store.repository.OrderRepository;
import com.example.store.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@ComponentScan(basePackageClasses = CustomerMapper.class)
class OrderControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private OrderService orderService;

    private Order order;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer();
        customer.setName("John Doe");
        customer.setId(1L);

        Product product = new Product();
        product.setId(1L);
        product.setDescription("Widget");

        order = new Order();
        order.setDescription("Test Order");
        order.setId(1L);
        order.setCustomer(customer);
        order.setProducts(Set.of(product));
    }

    private CreateOrderRequest request(List<Long> productIds) {
        CreateOrderRequest.CustomerRef customerRef = new CreateOrderRequest.CustomerRef();
        customerRef.setId(1L);

        List<CreateOrderRequest.ProductRef> products = productIds.stream()
                .map(id -> {
                    CreateOrderRequest.ProductRef ref = new CreateOrderRequest.ProductRef();
                    ref.setId(id);
                    return ref;
                })
                .toList();

        CreateOrderRequest request = new CreateOrderRequest();
        request.setDescription("Test Order");
        request.setCustomer(customerRef);
        request.setProducts(products);
        return request;
    }

    @Test
    void testCreateOrder() throws Exception {
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(order);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(List.of(1L)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Test Order"))
                .andExpect(jsonPath("$.customer.name").value("John Doe"))
                .andExpect(jsonPath("$.products[0].description").value("Widget"));
    }

    @Test
    void testCreateOrderWithoutProductsReturns400() throws Exception {
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(List.of()))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void testGetOrders() throws Exception {
        when(orderRepository.findAll()).thenReturn(List.of(order));

        mockMvc.perform(get("/order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Test Order"))
                .andExpect(jsonPath("$[0].customer.name").value("John Doe"))
                .andExpect(jsonPath("$[0].products[0].description").value("Widget"));
    }

    @Test
    void testGetOrderById() throws Exception {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        mockMvc.perform(get("/order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Test Order"));
    }

    @Test
    void testGetOrderByIdNotFound() throws Exception {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/order/99")).andExpect(status().isNotFound());
    }
}
