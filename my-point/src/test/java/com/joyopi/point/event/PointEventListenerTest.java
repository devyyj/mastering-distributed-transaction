package com.joyopi.point.event;

import com.joyopi.point.service.PointService;
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
class PointEventListenerTest {

    private PointEventListener pointEventListener;

    @Mock
    private PointService pointService;

    @BeforeEach
    void setUp() {
        pointEventListener = new PointEventListener(pointService, new ObjectMapper());
    }

    @Test
    @DisplayName("OrderCreatedEvent 수신 후 포인트를 차감하고 PointDeductedEvent를 아웃박스에 저장하도록 호출한다")
    void handle_order_created_event_success() {
        // given - OrderCreatedEvent JSON
        String message = "{\"orderId\":1,\"userId\":100,\"paymentAmount\":10000,\"usePoint\":1000}";
        doNothing().when(pointService).usePoint(1L, 100L, 1000L, 10000L);

        // when
        pointEventListener.handleOrderCreated(message);

        // then
        verify(pointService).usePoint(1L, 100L, 1000L, 10000L);
    }

    @Test
    @DisplayName("OrderCreatedEvent 수신 후 포인트 부족으로 실패 시 PointDeductionFailedEvent를 아웃박스에 저장하도록 호출한다")
    void handle_order_created_event_point_insufficient() {
        // given
        String message = "{\"orderId\":1,\"userId\":100,\"paymentAmount\":10000,\"usePoint\":1000}";
        doThrow(new RuntimeException("잔액 부족")).when(pointService).usePoint(1L, 100L, 1000L, 10000L);

        // when
        pointEventListener.handleOrderCreated(message);

        // then
        verify(pointService).usePoint(1L, 100L, 1000L, 10000L);
        verify(pointService).savePointDeductionFailedOutbox(1L, "잔액 부족");
    }

    @Test
    @DisplayName("PaymentFailedEvent 수신 후 포인트를 복원하고 PointRestoredEvent를 아웃박스에 저장하도록 호출한다")
    void handle_payment_failed_event() {
        // given - PaymentFailedEvent JSON (reason 있음)
        String message = "{\"orderId\":1,\"userId\":100,\"usePoint\":1000,\"reason\":\"결제 한도 초과\"}";
        doNothing().when(pointService).restorePoint(1L, 100L, 1000L);

        // when
        pointEventListener.handlePaymentFailed(message);

        // then
        verify(pointService).restorePoint(1L, 100L, 1000L);
    }

    @Test
    @DisplayName("PaymentFailedEvent 수신 시 reason, userId, 또는 usePoint가 null인 경우 무시한다")
    void handle_payment_failed_event_ignored_when_invalid() {
        // given - reason이 없는 유효하지 않은 메시지
        String message = "{\"orderId\":1,\"userId\":100,\"usePoint\":null,\"reason\":\"결제 한도 초과\"}";

        // when
        pointEventListener.handlePaymentFailed(message);

        // then
        org.mockito.Mockito.verifyNoInteractions(pointService);
    }

    @Test
    @DisplayName("PaymentFailedEvent 수신 시 포인트 복원 중 예외 발생 시 RuntimeException을 던진다")
    void handle_payment_failed_event_throws_exception_on_failure() {
        // given
        String message = "{\"orderId\":1,\"userId\":100,\"usePoint\":1000,\"reason\":\"결제 한도 초과\"}";
        doThrow(new RuntimeException("DB 연결 실패")).when(pointService).restorePoint(1L, 100L, 1000L);

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            pointEventListener.handlePaymentFailed(message);
        });
    }

    @Test
    @DisplayName("PaymentFailedEvent 수신 시 userId가 9999L이면 강제로 RuntimeException을 던져서 보상 트랜잭션을 실패하게 한다")
    void handle_payment_failed_event_forced_failure_for_test_user() {
        // given
        String message = "{\"orderId\":1,\"userId\":9999,\"usePoint\":1000,\"reason\":\"결제 실패\"}";

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            pointEventListener.handlePaymentFailed(message);
        });
    }
}
