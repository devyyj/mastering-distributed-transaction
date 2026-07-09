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
        String idempotencyKey = "idemp-key-pay-1";

        given(paymentRepository.findByIdempotencyKey(idempotencyKey)).willReturn(java.util.Optional.empty());
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(outboxRepository.save(any(OutboxEvent.class))).willReturn(null);

        // when
        paymentService.pay(orderId, userId, amount, usePoint, idempotencyKey);

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
        String idempotencyKey = "idemp-key-pay-2";

        given(paymentRepository.findByIdempotencyKey(idempotencyKey)).willReturn(java.util.Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentService.pay(orderId, userId, amount, usePoint, idempotencyKey))
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
        String idempotencyKey = "idemp-key-pay-3";

        given(paymentRepository.findByIdempotencyKey(idempotencyKey)).willReturn(java.util.Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentService.pay(orderId, userId, amount, usePoint, idempotencyKey))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_INPUT_VALUE.getMessage());
    }

    @Test
    @DisplayName("동일한 멱등키로 결제 승인이 중복 요청되면 비즈니스 로직을 실행하지 않고 리턴한다 (Idempotent Skip)")
    void pay_duplicate_shouldSkip() {
        // given
        Long orderId = 1L;
        Long userId = 100L;
        Long amount = 50000L;
        Long usePoint = 1000L;
        String idempotencyKey = "idemp-key-pay-dup";
        Payment existingPayment = Payment.createSuccess(orderId, amount, idempotencyKey);

        given(paymentRepository.findByIdempotencyKey(idempotencyKey)).willReturn(java.util.Optional.of(existingPayment));

        // when
        paymentService.pay(orderId, userId, amount, usePoint, idempotencyKey);

        // then
        // save나 outboxRepository가 호출되지 않아야 함
        org.mockito.Mockito.verify(paymentRepository, org.mockito.Mockito.never()).save(any(Payment.class));
        org.mockito.Mockito.verifyNoInteractions(outboxRepository);
    }

    @Test
    @DisplayName("결제 취소가 정상적으로 처리된다")
    void cancelPayment_success() {
        // given
        Long orderId = 1L;
        Long amount = 50000L;
        String idempotencyKey = "idemp-key-cancel-1";
        String cancelIdempotencyKey = idempotencyKey + "-cancel";

        given(paymentRepository.findByIdempotencyKey(cancelIdempotencyKey)).willReturn(java.util.Optional.empty());
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        paymentService.cancelPayment(orderId, amount, idempotencyKey);

        // then
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("동일한 멱등키로 결제 취소가 중복 요청되면 추가 취소 처리를 하지 않고 리턴한다 (Idempotent Skip)")
    void cancelPayment_duplicate_shouldSkip() {
        // given
        Long orderId = 1L;
        Long amount = 50000L;
        String idempotencyKey = "idemp-key-cancel-dup";
        String cancelIdempotencyKey = idempotencyKey + "-cancel";
        Payment existingCancel = Payment.createCanceled(orderId, amount, cancelIdempotencyKey);

        given(paymentRepository.findByIdempotencyKey(cancelIdempotencyKey)).willReturn(java.util.Optional.of(existingCancel));

        // when
        paymentService.cancelPayment(orderId, amount, idempotencyKey);

        // then
        org.mockito.Mockito.verify(paymentRepository, org.mockito.Mockito.never()).save(any(Payment.class));
    }
}
