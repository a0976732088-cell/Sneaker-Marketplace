package com.project.sneakermarket.commerce.repository;

import com.project.sneakermarket.commerce.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem,Long> {
    //找出某OrderId這張訂單底下有哪些商品明細
    List<OrderItem> findByOrderOrderId (Long OrderId);
}
