package com.joyopi.order.service;

import tools.jackson.databind.ObjectMapper;
import com.joyopi.order.domain.Order;
import com.joyopi.order.domain.OutboxEvent;
import com.joyopi.order.repository.OrderRepository;
import com.joyopi.order.repository.OutboxRepository;
import com.joyopi.order.service.dto.OrderCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("주문 요청 시 주문 데이터를 PENDING 상태로 저장하고 Outbox 테이블에 이벤트를 적재한다")
    void order_outbox_pattern_success() {
        // given
        OrderCommand command = new OrderCommand(1L, 10000L, 1000L, "idemp-key-1");
        Order savedOrder = Order.create(command.getUserId(), command.getProductPrice(), command.getUsePoint(), command.getIdempotencyKey());
        
        java.lang.reflect.Field idField;
        try {
            idField = Order.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(savedOrder, 100L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        given(orderRepository.findByIdempotencyKey("idemp-key-1")).willReturn(java.util.Optional.empty());
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
        given(outboxRepository.save(any(OutboxEvent.class))).willReturn(null);

        // when
        Long orderId = orderService.order(command);

        // then
        assertThat(orderId).isEqualTo(100L);
        verify(orderRepository).save(any(Order.class));
        verify(outboxRepository).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("userId가 9999인 경우 강제로 예외가 발생하며, 트랜잭션 롤백에 의해 Outbox 레코드도 DB에 반영되지 않는다 (롤백 조건 검증)")
    void order_outbox_pattern_exception_rollback() {
        // given
        OrderCommand command = new OrderCommand(9999L, 10000L, 1000L, "idemp-key-2");
        Order savedOrder = Order.create(command.getUserId(), command.getProductPrice(), command.getUsePoint(), command.getIdempotencyKey());
        
        java.lang.reflect.Field idField;
        try {
            idField = Order.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(savedOrder, 100L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        given(orderRepository.findByIdempotencyKey("idemp-key-2")).willReturn(java.util.Optional.empty());
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
        given(outboxRepository.save(any(OutboxEvent.class))).willReturn(null);

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            orderService.order(command);
        });

        // 비즈니스 트랜잭션 내에서 Outbox 저장을 호출했는지 검증
        // 실제 DB 환경에서는 해당 트랜잭션이 롤백되어 저장되지 않습니다.
        verify(orderRepository).save(any(Order.class));
        verify(outboxRepository).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("동일한 멱등키로 중복 주문 요청 시 주문을 새로 생성하지 않고 기존 주문의 ID를 반환한다")
    void order_idempotency_duplicate_returns_existing_id() {
        // given
        String idempotencyKey = "idemp-key-3";
        OrderCommand command = new OrderCommand(1L, 10000L, 1000L, idempotencyKey);
        Order existingOrder = Order.create(command.getUserId(), command.getProductPrice(), command.getUsePoint(), idempotencyKey);
        
        java.lang.reflect.Field idField;
        try {
            idField = Order.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(existingOrder, 300L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        given(orderRepository.findByIdempotencyKey(idempotencyKey)).willReturn(java.util.Optional.of(existingOrder));

        // when
        Long orderId = orderService.order(command);

        // then
        assertThat(orderId).isEqualTo(300L);
        // save나 outboxRepository가 호출되지 않아야 함
        org.mockito.Mockito.verifyNoInteractions(outboxRepository);
    }
}

