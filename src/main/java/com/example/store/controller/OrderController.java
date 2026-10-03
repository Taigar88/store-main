package com.example.store.controller;

import com.example.store.dto.CreateOrderRequest;
import com.example.store.dto.OrderDTO;
import com.example.store.mapper.OrderMapper;
import com.example.store.repository.OrderRepository;
import com.example.store.service.OrderService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @GetMapping
    public List<OrderDTO> getOrders() {
        return orderMapper.ordersToOrderDTOs(orderRepository.findAll());
    }

    @GetMapping("/{id}")
    public OrderDTO getOrderById(@PathVariable Long id) {
        return orderRepository
                .findById(id)
                .map(orderMapper::orderToOrderDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order " + id + " not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDTO createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return orderMapper.orderToOrderDTO(orderService.createOrder(request));
    }
}
