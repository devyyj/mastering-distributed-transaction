package com.joyopi.monolith.order.repository;

import com.joyopi.monolith.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 주문 레포지토리
 */
public interface OrderRepository extends JpaRepository<Order, Long> {
}
