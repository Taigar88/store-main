package com.example.store.repository;

import com.example.store.entity.Order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Override
    @EntityGraph(attributePaths = {"customer", "products"})
    List<Order> findAll();

    @Override
    @EntityGraph(attributePaths = {"customer", "products"})
    Optional<Order> findById(Long id);
}
