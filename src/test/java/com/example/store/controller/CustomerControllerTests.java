package com.example.store.controller;

import com.example.store.entity.Customer;
import com.example.store.mapper.CustomerMapper;
import com.example.store.repository.CustomerRepository;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
@ComponentScan(basePackageClasses = CustomerMapper.class)
class CustomerControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerRepository customerRepository;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setName("John Doe");
        customer.setId(1L);
    }

    @Test
    void testCreateCustomer() throws Exception {
        // The controller builds its own Customer from the request, so match any instance
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "John Doe"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    void testCreateCustomerWithBlankNameReturns400() throws Exception {
        mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "  "))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(customerRepository);
    }

    @Test
    void testGetAllCustomers() throws Exception {
        when(customerRepository.findAll()).thenReturn(List.of(customer));

        mockMvc.perform(get("/customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("John Doe"));
    }

    @Test
    void testSearchCustomersByName() throws Exception {
        when(customerRepository.findByNameContainingIgnoreCase("john")).thenReturn(List.of(customer));

        mockMvc.perform(get("/customer").param("name", " john "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("John Doe"));
    }
}
