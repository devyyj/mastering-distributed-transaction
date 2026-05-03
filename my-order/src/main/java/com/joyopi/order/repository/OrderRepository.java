package com.joyopi.order.repository;

import com.joyopi.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 주문 레포지토리
 */
public interface OrderRepository extends JpaRepository<Order, Long> {
}
