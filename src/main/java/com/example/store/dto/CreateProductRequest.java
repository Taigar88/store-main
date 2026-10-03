package com.example.store.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class CreateProductRequest {
    @NotBlank
    private String description;
}
