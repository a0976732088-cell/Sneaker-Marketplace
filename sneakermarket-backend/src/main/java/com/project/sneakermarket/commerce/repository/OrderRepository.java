package com.project.sneakermarket.commerce.repository;

import com.project.sneakermarket.commerce.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order,Long> {
    //用 memberId 找出這個會員的訂單
    List<Order> findByMemberId(Long memberId);
}
