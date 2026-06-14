package com.joyopi.payment.event;

import com.joyopi.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @InjectMocks
    private PaymentEventListener paymentEventListener;

    @Mock
    private PaymentService paymentService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("OrderCreatedEvent 수신 시 결제를 진행하고 PaymentApprovedEvent를 발행한다")
    void handle_order_created_event() {
        // given
        OrderCreatedEvent event = new OrderCreatedEvent(1L, 100L, 10000L, 1000L);
        doNothing().when(paymentService).pay(1L, 10000L);

        // when
        paymentEventListener.handleOrderCreated(event);

        // then
        verify(paymentService).pay(1L, 10000L);
        verify(kafkaTemplate).send(eq("payment-events"), any(PaymentApprovedEvent.class));
    }

    @Test
    @DisplayName("PointDeductionFailedEvent 수신 시 결제를 취소하고 PaymentCancelledEvent를 발행한다")
    void handle_point_deduction_failed_event() {
        // given
        PointDeductionFailedEvent event = new PointDeductionFailedEvent(1L, "포인트 부족");
        // 결제 금액을 알아내기 위해 실제 DB 조회가 필요하거나, 서비스 계층에서 orderId 기반으로 조회하여 처리하므로 Mocking 설정
        doNothing().when(paymentService).cancelPayment(1L, 0L); // 예시로 0L을 던지거나 별도의 주문 상태 조회 처리

        // when
        paymentEventListener.handlePointDeductionFailed(event);

        // then
        verify(paymentService).cancelPayment(1L, 0L);
        verify(kafkaTemplate).send(eq("payment-events"), any(PaymentCancelledEvent.class));
    }
}
