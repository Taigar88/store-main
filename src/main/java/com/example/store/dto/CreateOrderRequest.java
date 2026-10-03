package com.example.store.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {
    @NotBlank
    private String description;

    @NotNull @Valid
    private CustomerRef customer;

    @NotEmpty
    private List<@NotNull @Valid ProductRef> products;

    @Data
    public static class CustomerRef {
        @NotNull private Long id;
    }

    @Data
    public static class ProductRef {
        @NotNull private Long id;
    }
}
