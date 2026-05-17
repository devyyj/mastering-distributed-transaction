package com.joyopi.order.repository;

import com.joyopi.order.domain.Order;
import com.joyopi.order.domain.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("Order 엔티티가 정상적으로 저장되고 조회된다")
    void saveAndFindOrder() {
        // given
        Order order = Order.create(1L, 100000L, 2000L);
        order.reserve();

        // when
        Order savedOrder = orderRepository.save(order);

        // then
        assertThat(savedOrder.getId()).isNotNull();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.RESERVED);

        Order foundOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();
        assertThat(foundOrder.getStatus()).isEqualTo(OrderStatus.RESERVED);
    }
}
