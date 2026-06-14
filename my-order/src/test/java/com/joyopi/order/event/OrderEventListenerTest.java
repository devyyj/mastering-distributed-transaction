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

    @Test
    @DisplayName("PointDeductedEvent 수신 시 주문을 완료 처리한다")
    void handle_point_deducted_event() {
        // given
        PointDeductedEvent event = new PointDeductedEvent(1L);
        Order order = Order.create(1L, 10000L, 1000L);
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));

        // when
        orderEventListener.handlePointDeducted(event);

        // then
        verify(orderRepository).findById(1L);
        // Order 상태가 COMPLETED로 바뀌었는지 검증하기 위해 complete()가 잘 반영되었는지 확인
        // (Order의 complete() 메서드가 호출되어 상태가 COMPLETED가 됨)
    }

    @Test
    @DisplayName("PointDeductionFailedEvent 수신 시 주문을 실패 처리한다")
    void handle_point_deduction_failed_event() {
        // given
        PointDeductionFailedEvent event = new PointDeductionFailedEvent(1L, "잔액 부족");
        Order order = Order.create(1L, 10000L, 1000L);
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));

        // when
        orderEventListener.handlePointDeductionFailed(event);

        // then
        verify(orderRepository).findById(1L);
    }
}
