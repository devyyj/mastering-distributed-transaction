package com.joyopi.order.service;

import com.joyopi.order.domain.Order;
import com.joyopi.order.repository.OrderRepository;
import com.joyopi.order.service.dto.OrderCommand;
import com.joyopi.order.event.OrderCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("주문 요청 시 주문 데이터를 PENDING 상태로 저장하고 OrderCreatedEvent 이벤트를 발행한다")
    void order_choreography_start() {
        // given
        OrderCommand command = new OrderCommand(1L, 10000L, 1000L);
        Order savedOrder = Order.create(command.getUserId(), command.getProductPrice(), command.getUsePoint());
        // Reflection 혹은 빌더 등을 통해 ID가 세팅된 Order 객체 시뮬레이션
        // JPA 저장 시 ID가 매핑되므로 Mockito로 저장 시 ID가 부여된 savedOrder를 반환하도록 함
        java.lang.reflect.Field idField;
        try {
            idField = Order.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(savedOrder, 100L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
        given(kafkaTemplate.send(eq("order-events"), any(OrderCreatedEvent.class)))
                .willReturn(null); // return 값은 CompletableFuture가 올 수 있지만 Spring Kafka API 호출 확인 용도

        // when
        Long orderId = orderService.order(command);

        // then
        assertThat(orderId).isEqualTo(100L);
        verify(orderRepository).save(any(Order.class));
        verify(kafkaTemplate).send(eq("order-events"), any(OrderCreatedEvent.class));
    }
}

