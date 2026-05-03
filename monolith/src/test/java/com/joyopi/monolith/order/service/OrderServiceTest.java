package com.joyopi.monolith.order.service;

import com.joyopi.monolith.common.exception.BusinessException;
import com.joyopi.monolith.common.exception.ErrorCode;
import com.joyopi.monolith.order.domain.Order;
import com.joyopi.monolith.order.repository.OrderRepository;
import com.joyopi.monolith.order.domain.OrderStatus;
import com.joyopi.monolith.order.service.dto.OrderCommand;
import com.joyopi.monolith.payment.service.PaymentService;
import com.joyopi.monolith.point.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private PointService pointService;

    @Mock
    private PaymentService paymentService;

    @Test
    @DisplayName("주문 요청이 들어오면 포인트 차감, 결제 처리 후 주문이 완료된다")
    void order_success() {
        // given
        OrderCommand command = OrderCommand.builder()
                .userId(1L)
                .productPrice(10000L)
                .usePoint(2000L)
                .build();

        Order order = Order.create(command.getUserId(), command.getProductPrice(), command.getUsePoint());
        given(orderRepository.save(any(Order.class))).willReturn(order);

        // when
        Long orderId = orderService.order(command);

        // then
        verify(pointService).usePoint(command.getUserId(), command.getUsePoint());
        verify(paymentService).pay(any(), any());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("상품 가격이 음수이면 예외가 발생한다")
    void order_negativeProductPrice() {
        // given
        OrderCommand command = OrderCommand.builder()
                .userId(1L)
                .productPrice(-1000L)
                .usePoint(0L)
                .build();

        // when & then
        assertThatThrownBy(() -> orderService.order(command))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_INPUT_VALUE.getMessage());
    }
}
