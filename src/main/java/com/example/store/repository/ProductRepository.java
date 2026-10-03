package com.example.store.repository;

import com.example.store.entity.Product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    interface ProductOrderId {
        Long getProductId();

        Long getOrderId();
    }

    @Query(
            value = "select product_id as productId, order_id as orderId from order_product order by order_id",
            nativeQuery = true)
    List<ProductOrderId> findAllProductOrderIds();

    @Query(
            value = "select order_id from order_product where product_id = :productId order by order_id",
            nativeQuery = true)
    List<Long> findOrderIdsByProductId(@Param("productId") Long productId);
}
