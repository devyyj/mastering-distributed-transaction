package com.joyopi.order.service;

import com.joyopi.order.common.exception.BusinessException;
import com.joyopi.order.common.exception.ErrorCode;
import com.joyopi.order.domain.Order;
import com.joyopi.order.domain.OrderStatus;
import com.joyopi.order.repository.OrderRepository;
import com.joyopi.order.service.dto.OrderCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RestTemplate restTemplate;

    @Test
    @DisplayName("TCC: 주문 요청이 들어오면 모든 Try 성공 후 Confirm이 호출되고 주문이 완료된다")
    void order_success() throws Exception {
        // given
        OrderCommand command = OrderCommand.builder()
                .userId(1L)
                .productPrice(10000L)
                .usePoint(2000L)
                .build();

        Order order = Order.create(command.getUserId(), command.getProductPrice(), command.getUsePoint());
        java.lang.reflect.Field idField = Order.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(order, 1L);

        given(orderRepository.save(any(Order.class))).willReturn(order);
        given(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .willReturn(ResponseEntity.ok().build());

        // when
        Long orderId = orderService.order(command);

        // then
        // Try 단계 호출 확인
        verify(restTemplate).postForEntity(contains("points/try"), any(), eq(Void.class));
        verify(restTemplate).postForEntity(contains("payments/try"), any(), eq(Void.class));
        
        // Confirm 단계 호출 확인
        verify(restTemplate).postForEntity(contains("points/confirm"), any(), eq(Void.class));
        verify(restTemplate).postForEntity(contains("payments/confirm"), any(), eq(Void.class));
        
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("TCC: 결제 Try 실패 시 포인트 Cancel API를 호출하고 주문 상태가 CANCELLED가 된다")
    void order_paymentTryFail_cancel() throws Exception {
        // given
        OrderCommand command = OrderCommand.builder()
                .userId(1L)
                .productPrice(10000L)
                .usePoint(2000L)
                .build();

        Order order = Order.create(command.getUserId(), command.getProductPrice(), command.getUsePoint());
        java.lang.reflect.Field idField = Order.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(order, 1L);

        given(orderRepository.save(any(Order.class))).willReturn(order);
        
        // [Try] 포인트 예약 성공
        given(restTemplate.postForEntity(contains("points/try"), any(), eq(Void.class)))
                .willReturn(ResponseEntity.ok().build());
        
        // [Try] 결제 예약 실패
        given(restTemplate.postForEntity(contains("payments/try"), any(), eq(Void.class)))
                .willThrow(new RuntimeException("Payment try failed"));

        // [Cancel] 포인트 예약 취소 성공
        given(restTemplate.postForEntity(contains("points/cancel"), any(), eq(Void.class)))
                .willReturn(ResponseEntity.ok().build());

        // when & then
        assertThatThrownBy(() -> orderService.order(command))
                .isInstanceOf(RuntimeException.class);

        // then
        verify(restTemplate).postForEntity(contains("points/cancel"), any(), eq(Void.class));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

}
