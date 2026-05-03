package com.joyopi.payment.service;

import com.joyopi.payment.common.exception.BusinessException;
import com.joyopi.payment.common.exception.ErrorCode;
import com.joyopi.payment.domain.Payment;
import com.joyopi.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 서비스
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;

    /**
     * 결제 요청 처리
     */
    @Transactional
    public void pay(Long orderId, Long amount) {
        if (amount < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (amount > 1000000) {
            throw new BusinessException(ErrorCode.PAYMENT_LIMIT_EXCEEDED);
        }

        // 외부 결제 API 시뮬레이션 (딜레이)
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }

        Payment payment = Payment.createSuccess(orderId, amount);
        paymentRepository.save(payment);
    }
}
