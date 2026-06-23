package com.joyopi.payment.service;

import tools.jackson.databind.ObjectMapper;
import com.joyopi.payment.common.exception.BusinessException;
import com.joyopi.payment.common.exception.ErrorCode;
import com.joyopi.payment.domain.Payment;
import com.joyopi.payment.domain.OutboxEvent;
import com.joyopi.payment.repository.PaymentRepository;
import com.joyopi.payment.repository.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentHistoryService paymentHistoryService;

    @Mock
    private OutboxRepository outboxRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("결제 요청이 정상적으로 처리되고 아웃박스 이벤트가 저장된다")
    void pay_success() {
        // given
        Long orderId = 1L;
        Long userId = 100L;
        Long amount = 50000L;
        Long usePoint = 1000L;
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(outboxRepository.save(any(OutboxEvent.class))).willReturn(null);

        // when
        paymentService.pay(orderId, userId, amount, usePoint);

        // then
        verify(paymentRepository).save(any(Payment.class));
        verify(outboxRepository).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("결제 한도(1,000,000원)를 초과하면 예외가 발생한다")
    void pay_limitExceeded() {
        // given
        Long orderId = 1L;
        Long userId = 100L;
        Long amount = 1000001L;
        Long usePoint = 1000L;

        // when & then
        assertThatThrownBy(() -> paymentService.pay(orderId, userId, amount, usePoint))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PAYMENT_LIMIT_EXCEEDED.getMessage());
    }

    @Test
    @DisplayName("음수 금액 결제 요청 시 예외가 발생한다")
    void pay_negativeAmount() {
        // given
        Long orderId = 1L;
        Long userId = 100L;
        Long amount = -100L;
        Long usePoint = 1000L;

        // when & then
        assertThatThrownBy(() -> paymentService.pay(orderId, userId, amount, usePoint))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_INPUT_VALUE.getMessage());
    }
}
