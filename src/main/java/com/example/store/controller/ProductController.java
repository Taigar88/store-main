package com.example.store.controller;

import com.example.store.dto.CreateProductRequest;
import com.example.store.dto.ProductDTO;
import com.example.store.service.ProductService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductDTO> getProducts() {
        return productService.findAll();
    }

    @GetMapping("/{id}")
    public ProductDTO getProductById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDTO createProduct(@Valid @RequestBody CreateProductRequest request) {
        return productService.create(request);
    }
}
