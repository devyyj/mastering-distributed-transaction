package com.joyopi.payment.service;

import com.joyopi.payment.common.exception.BusinessException;
import com.joyopi.payment.common.exception.ErrorCode;
import com.joyopi.payment.domain.Payment;
import com.joyopi.payment.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

    @Test
    @DisplayName("결제 요청이 정상적으로 처리된다")
    void pay_success() {
        // given
        Long orderId = 1L;
        Long amount = 50000L;
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        paymentService.pay(orderId, amount);

        // then
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("결제 한도(1,000,000원)를 초과하면 예외가 발생하고 실패 상태가 저장된다")
    void pay_limitExceeded() {
        // given
        Long orderId = 1L;
        Long amount = 1000001L;

        // when & then
        assertThatThrownBy(() -> paymentService.pay(orderId, amount))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PAYMENT_LIMIT_EXCEEDED.getMessage());
        
        verify(paymentHistoryService).saveFailedPayment(orderId, amount);
    }

    @Test
    @DisplayName("음수 금액 결제 요청 시 예외가 발생하고 실패 상태가 저장된다")
    void pay_negativeAmount() {
        // given
        Long orderId = 1L;
        Long amount = -100L;

        // when & then
        assertThatThrownBy(() -> paymentService.pay(orderId, amount))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_INPUT_VALUE.getMessage());

        verify(paymentHistoryService).saveFailedPayment(orderId, amount);
    }
}
