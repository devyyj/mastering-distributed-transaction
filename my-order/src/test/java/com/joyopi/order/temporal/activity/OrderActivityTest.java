package com.joyopi.order.temporal.activity;

import com.joyopi.order.domain.Order;
import com.joyopi.order.domain.OrderStatus;
import com.joyopi.order.repository.OrderRepository;
import com.joyopi.order.service.dto.OrderCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderActivityTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderActivityImpl orderActivity;

    @Test
    @DisplayName("주문 대기 상태 생성 Activity")
    void createPendingOrder() {
        // given
        OrderCommand command = new OrderCommand(1L, 10000L, 1000L);
        Order savedOrder = Order.create(1L, 10000L, 1000L);
        // ReflectionTestUtils.setField(savedOrder, "id", 100L); // 만약 id가 필요하면
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

        // when
        OrderActivity.OrderResult result = orderActivity.createPendingOrder(command);

        // then
        assertThat(result).isNotNull();
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 완료 Activity")
    void completeOrder() {
        // given
        Long orderId = 1L;
        Order order = Order.create(1L, 10000L, 1000L);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        // when
        orderActivity.completeOrder(orderId);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("주문 취소(보상) Activity")
    void cancelOrder() {
        // given
        Long orderId = 1L;
        Order order = Order.create(1L, 10000L, 1000L);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        // when
        orderActivity.cancelOrder(orderId);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
    }
}
