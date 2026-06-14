package com.joyopi.point.event;

import com.joyopi.point.service.PointService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PointEventListenerTest {

    @InjectMocks
    private PointEventListener pointEventListener;

    @Mock
    private PointService pointService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("PaymentApprovedEvent 수신 시 포인트를 차감하고 PointDeductedEvent를 발행한다")
    void handle_payment_approved_event_success() {
        // given
        PaymentApprovedEvent event = new PaymentApprovedEvent(1L, 2L, 100L, 1000L);
        doNothing().when(pointService).usePoint(100L, 1000L);

        // when
        pointEventListener.handlePaymentApproved(event);

        // then
        verify(pointService).usePoint(100L, 1000L);
        verify(kafkaTemplate).send(eq("point-events"), any(PointDeductedEvent.class));
    }

    @Test
    @DisplayName("PaymentApprovedEvent 수신 후 포인트 부족 등 실패 시 PointDeductionFailedEvent를 발행한다")
    void handle_payment_approved_event_fail() {
        // given
        PaymentApprovedEvent event = new PaymentApprovedEvent(1L, 2L, 100L, 1000L);
        doThrow(new RuntimeException("잔액 부족")).when(pointService).usePoint(100L, 1000L);

        // when
        pointEventListener.handlePaymentApproved(event);

        // then
        verify(pointService).usePoint(100L, 1000L);
        verify(kafkaTemplate).send(eq("point-events"), any(PointDeductionFailedEvent.class));
    }
}
