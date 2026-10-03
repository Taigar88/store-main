package com.example.store.repository;

import com.example.store.entity.Customer;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Override
    @EntityGraph(attributePaths = "orders")
    List<Customer> findAll();

    @EntityGraph(attributePaths = "orders")
    List<Customer> findByNameContainingIgnoreCase(String name);
}
