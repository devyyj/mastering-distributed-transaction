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
    @DisplayName("주문 요청이 들어오면 포인트 차감, 결제 처리 후 주문이 완료된다")
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
        verify(restTemplate).postForEntity(contains("points"), any(), eq(Void.class));
        verify(restTemplate).postForEntity(contains("payments"), any(), eq(Void.class));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("결제 처리 중 예외 발생 시 포인트 복구 API를 호출하고 주문 상태가 FAIL이 된다")
    void order_paymentFail_compensation() throws Exception {
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
        
        // 포인트 차감 성공
        given(restTemplate.postForEntity(contains("points/use"), any(), eq(Void.class)))
                .willReturn(ResponseEntity.ok().build());
        
        // 결제 처리 실패
        given(restTemplate.postForEntity(contains("payments"), any(), eq(Void.class)))
                .willThrow(new RuntimeException("Payment failed"));

        // 포인트 복구 성공
        given(restTemplate.postForEntity(contains("points/restore"), any(), eq(Void.class)))
                .willReturn(ResponseEntity.ok().build());

        // when & then
        assertThatThrownBy(() -> orderService.order(command))
                .isInstanceOf(RuntimeException.class);

        // then
        verify(restTemplate).postForEntity(contains("points/restore"), any(), eq(Void.class));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
    }
}
