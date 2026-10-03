package com.example.store.controller;

import com.example.store.dto.CreateCustomerRequest;
import com.example.store.dto.CustomerDTO;
import com.example.store.entity.Customer;
import com.example.store.mapper.CustomerMapper;
import com.example.store.repository.CustomerRepository;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @GetMapping
    public List<CustomerDTO> getCustomers(@RequestParam(required = false) String name) {
        List<Customer> customers = (name == null || name.isBlank())
                ? customerRepository.findAll()
                : customerRepository.findByNameContainingIgnoreCase(name.trim());
        return customerMapper.customersToCustomerDTOs(customers);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDTO createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = new Customer();
        customer.setName(request.getName());
        return customerMapper.customerToCustomerDTO(customerRepository.save(customer));
    }
}
