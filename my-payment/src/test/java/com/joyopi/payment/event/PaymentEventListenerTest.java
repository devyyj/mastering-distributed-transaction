package com.joyopi.payment.event;

import com.joyopi.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    private PaymentEventListener paymentEventListener;

    @Mock
    private PaymentService paymentService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setUp() {
        // ObjectMapper는 실제 인스턴스 사용 (tools.jackson.databind.ObjectMapper는 Mockito Mock 불가)
        paymentEventListener = new PaymentEventListener(paymentService, kafkaTemplate, new ObjectMapper());
    }

    @Test
    @DisplayName("PointDeductedEvent 수신 후 결제를 진행하고 PaymentApprovedEvent를 발행한다")
    void handle_point_deducted_event_success() {
        // given - PointDeductedEvent JSON (userId 있음)
        String message = "{\"orderId\":1,\"userId\":100,\"paymentAmount\":10000,\"usePoint\":1000}";
        doNothing().when(paymentService).pay(1L, 10000L);

        // when
        paymentEventListener.handlePointDeducted(message);

        // then
        verify(paymentService).pay(1L, 10000L);
        verify(kafkaTemplate).send(eq("payment-events"), any(PaymentApprovedEvent.class));
    }

    @Test
    @DisplayName("PointDeductedEvent 수신 후 결제 실패 시 PaymentFailedEvent를 발행한다")
    void handle_point_deducted_event_payment_fail() {
        // given
        String message = "{\"orderId\":1,\"userId\":100,\"paymentAmount\":10000,\"usePoint\":1000}";
        doThrow(new RuntimeException("결제 한도 초과")).when(paymentService).pay(1L, 10000L);

        // when
        paymentEventListener.handlePointDeducted(message);

        // then
        verify(paymentService).pay(1L, 10000L);
        verify(kafkaTemplate).send(eq("payment-events"), any(PaymentFailedEvent.class));
    }

    @Test
    @DisplayName("userId 또는 paymentAmount가 null인 유효하지 않은 이벤트 수신 시 무시한다")
    void handle_point_deducted_event_ignored_when_invalid() {
        // given
        String message = "{\"orderId\":1,\"userId\":null,\"paymentAmount\":10000,\"usePoint\":1000}";

        // when
        paymentEventListener.handlePointDeducted(message);

        // then
        // pay()가 호출되지 않고 kafkaTemplate.send()가 호출되지 않아야 함
        org.mockito.Mockito.verifyNoInteractions(paymentService);
        org.mockito.Mockito.verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("PointRestoredEvent 수신 시 paymentAmount가 null이므로 무시한다")
    void handle_point_deducted_event_ignored_when_point_restored_event() {
        // given
        String message = "{\"orderId\":33,\"userId\":1,\"restoredAmount\":2000}";

        // when
        paymentEventListener.handlePointDeducted(message);

        // then
        org.mockito.Mockito.verifyNoInteractions(paymentService);
        org.mockito.Mockito.verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("PointDeductedEvent 수신 후 결제 실패 시 PaymentFailedEvent 발행조차 실패하면 RuntimeException을 던진다")
    void handle_point_deducted_event_payment_fail_throws_exception_on_event_send_fail() {
        // given
        String message = "{\"orderId\":1,\"userId\":100,\"paymentAmount\":10000,\"usePoint\":1000}";
        doThrow(new RuntimeException("결제 한도 초과")).when(paymentService).pay(1L, 10000L);
        doThrow(new RuntimeException("Kafka Broker Down")).when(kafkaTemplate).send(eq("payment-events"), any(PaymentFailedEvent.class));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            paymentEventListener.handlePointDeducted(message);
        });
    }
}
