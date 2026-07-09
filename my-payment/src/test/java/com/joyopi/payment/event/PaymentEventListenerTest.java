package com.joyopi.payment.event;

import com.joyopi.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    @BeforeEach
    void setUp() {
        paymentEventListener = new PaymentEventListener(paymentService, new ObjectMapper());
    }

    @Test
    @DisplayName("PointDeductedEvent 수신 후 결제를 진행하고 PaymentApprovedEvent를 아웃박스에 적재하도록 호출한다")
    void handle_point_deducted_event_success() {
        // given - PointDeductedEvent JSON (userId 있음)
        String message = "{\"orderId\":1,\"userId\":100,\"paymentAmount\":10000,\"usePoint\":1000,\"idempotencyKey\":\"idemp-key-1\"}";
        doNothing().when(paymentService).pay(1L, 100L, 10000L, 1000L, "idemp-key-1");

        // when
        paymentEventListener.handlePointDeducted(message);

        // then
        verify(paymentService).pay(1L, 100L, 10000L, 1000L, "idemp-key-1");
    }

    @Test
    @DisplayName("PointDeductedEvent 수신 후 결제 실패 시 PaymentFailedEvent를 아웃박스에 적재하도록 호출한다")
    void handle_point_deducted_event_payment_fail() {
        // given
        String message = "{\"orderId\":1,\"userId\":100,\"paymentAmount\":10000,\"usePoint\":1000,\"idempotencyKey\":\"idemp-key-2\"}";
        doThrow(new RuntimeException("결제 한도 초과")).when(paymentService).pay(1L, 100L, 10000L, 1000L, "idemp-key-2");

        // when
        paymentEventListener.handlePointDeducted(message);

        // then
        verify(paymentService).pay(1L, 100L, 10000L, 1000L, "idemp-key-2");
        verify(paymentService).savePaymentFailedOutbox(1L, 100L, 1000L, "결제 한도 초과", "idemp-key-2");
    }

    @Test
    @DisplayName("userId 또는 paymentAmount가 null인 유효하지 않은 이벤트 수신 시 무시한다")
    void handle_point_deducted_event_ignored_when_invalid() {
        // given
        String message = "{\"orderId\":1,\"userId\":null,\"paymentAmount\":10000,\"usePoint\":1000}";

        // when
        paymentEventListener.handlePointDeducted(message);

        // then
        org.mockito.Mockito.verifyNoInteractions(paymentService);
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
    }
}
