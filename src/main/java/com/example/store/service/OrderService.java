package com.example.store.service;

import com.example.store.dto.CreateOrderRequest;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.entity.Product;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Long customerId = request.getCustomer().getId();
        Customer customer = customerRepository
                .findById(customerId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer " + customerId + " not found"));

        Set<Long> productIds = new HashSet<>();
        request.getProducts().forEach(p -> productIds.add(p.getId()));
        List<Product> products = productRepository.findAllById(productIds);
        if (products.size() != productIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more products not found");
        }

        Order order = new Order();
        order.setDescription(request.getDescription());
        order.setCustomer(customer);
        order.setProducts(new HashSet<>(products));
        return orderRepository.save(order);
    }
}
