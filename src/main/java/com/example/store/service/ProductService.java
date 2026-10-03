package com.example.store.service;

import com.example.store.dto.CreateProductRequest;
import com.example.store.dto.ProductDTO;
import com.example.store.entity.Product;
import com.example.store.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductDTO create(CreateProductRequest request) {
        Product product = new Product();
        product.setDescription(request.getDescription());
        Product saved = productRepository.save(product);
        return new ProductDTO(saved.getId(), saved.getDescription(), List.of());
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> findAll() {
        Map<Long, List<Long>> orderIdsByProduct = productRepository.findAllProductOrderIds().stream()
                .collect(Collectors.groupingBy(
                        ProductRepository.ProductOrderId::getProductId,
                        Collectors.mapping(ProductRepository.ProductOrderId::getOrderId, Collectors.toList())));

        return productRepository.findAll().stream()
                .map(p -> new ProductDTO(
                        p.getId(), p.getDescription(), orderIdsByProduct.getOrDefault(p.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductDTO findById(Long id) {
        Product product = productRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product " + id + " not found"));
        return new ProductDTO(product.getId(), product.getDescription(), productRepository.findOrderIdsByProductId(id));
    }
}
