package com.joyopi.order.event;

import com.joyopi.order.domain.Order;
import com.joyopi.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @InjectMocks
    private OrderEventListener orderEventListener;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private tools.jackson.databind.ObjectMapper objectMapper;

    @Test
    @DisplayName("PaymentApprovedEvent 수신 후 주문을 완료 처리한다")
    void handle_payment_approved_event() throws Exception {
        // given - PaymentApprovedEvent JSON (reason 필드 없음)
        String message = "{\"orderId\":1,\"paymentId\":1,\"userId\":100}";
        Order order = Order.create(1L, 10000L, 1000L);

        tools.jackson.databind.ObjectMapper realMapper = new tools.jackson.databind.ObjectMapper();
        given(objectMapper.readTree(message)).willReturn(realMapper.readTree(message));
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));

        // when
        orderEventListener.handlePaymentEvent(message);

        // then
        verify(orderRepository).findById(1L);
        // order.complete()가 호출되어 상태가 COMPLETED가 됨
    }

    @Test
    @DisplayName("PaymentFailedEvent 수신 후 주문을 실패 처리한다")
    void handle_payment_failed_event() throws Exception {
        // given - PaymentFailedEvent JSON (reason 필드 있음)
        String message = "{\"orderId\":1,\"userId\":100,\"reason\":\"결제 한도 초과\"}";
        Order order = Order.create(1L, 10000L, 1000L);

        tools.jackson.databind.ObjectMapper realMapper = new tools.jackson.databind.ObjectMapper();
        given(objectMapper.readTree(message)).willReturn(realMapper.readTree(message));
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));

        // when
        orderEventListener.handlePaymentEvent(message);

        // then
        verify(orderRepository).findById(1L);
        // order.fail()이 호출되어 상태가 FAILED가 됨
    }

    @Test
    @DisplayName("주문 완료 처리 중 DB 조회 실패 시 RuntimeException을 던진다")
    void handle_payment_event_throws_exception_on_db_error() throws Exception {
        // given
        String message = "{\"orderId\":1,\"paymentId\":1,\"userId\":100}";
        tools.jackson.databind.ObjectMapper realMapper = new tools.jackson.databind.ObjectMapper();
        given(objectMapper.readTree(message)).willReturn(realMapper.readTree(message));
        given(orderRepository.findById(1L)).willThrow(new RuntimeException("DB Connection Timeout"));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            orderEventListener.handlePaymentEvent(message);
        });
    }

    @Test
    @DisplayName("PointDeductionFailedEvent 수신 후 주문을 실패 처리한다")
    void handle_point_deduction_failed_event() throws Exception {
        // given - PointDeductionFailedEvent JSON (reason 필드 있음, userId 없음)
        String message = "{\"orderId\":1,\"reason\":\"포인트 부족\"}";
        Order order = Order.create(1L, 10000L, 1000L);

        tools.jackson.databind.ObjectMapper realMapper = new tools.jackson.databind.ObjectMapper();
        given(objectMapper.readTree(message)).willReturn(realMapper.readTree(message));
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));

        // when
        orderEventListener.handlePointEvent(message);

        // then
        verify(orderRepository).findById(1L);
        org.junit.jupiter.api.Assertions.assertEquals(com.joyopi.order.domain.OrderStatus.FAILED, order.getStatus());
    }
}
