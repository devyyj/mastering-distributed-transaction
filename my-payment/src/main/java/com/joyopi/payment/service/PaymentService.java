package com.joyopi.payment.service;

import com.joyopi.payment.common.exception.BusinessException;
import com.joyopi.payment.common.exception.ErrorCode;
import com.joyopi.payment.domain.Payment;
import com.joyopi.payment.domain.PaymentStatus;
import com.joyopi.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 서비스
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentHistoryService paymentHistoryService;

    /**
     * TCC - Try: 결제 예약
     */
    @Transactional
    public void tryPay(Long orderId, Long amount) {
        log.info("TCC-Try: 결제 예약 시작 - orderId: {}, amount: {}", orderId, amount);

        try {
            validateAmount(amount);

            // 외부 결제 API 시뮬레이션 (딜레이) - Try 단계에서 가용한지 확인
            simulateExternalPayment();

            Payment payment = Payment.createReserved(orderId, amount);
            paymentRepository.save(payment);
            log.info("TCC-Try: 결제 예약 성공 - orderId: {}", orderId);

        } catch (BusinessException e) {
            log.error("TCC-Try: 결제 예약 중 비즈니스 예외 발생 - orderId: {}, message: {}", orderId, e.getMessage());
            paymentHistoryService.saveFailedPayment(orderId, amount);
            throw e;
        } catch (Exception e) {
            log.error("TCC-Try: 결제 예약 중 예상치 못한 예외 발생 - orderId: {}", orderId, e);
            paymentHistoryService.saveFailedPayment(orderId, amount);
            throw e;
        }
    }

    /**
     * TCC - Confirm: 결제 확정
     */
    @Transactional
    public void confirmPay(Long orderId) {
        log.info("TCC-Confirm: 결제 확정 시작 - orderId: {}", orderId);
        Payment payment = paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.RESERVED)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        payment.confirm();
        log.info("TCC-Confirm: 결제 확정 완료 - orderId: {}", orderId);
    }

    /**
     * TCC - Cancel: 결제 취소
     */
    @Transactional
    public void cancelPay(Long orderId) {
        log.info("TCC-Cancel: 결제 취소 시작 - orderId: {}", orderId);
        Payment payment = paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.RESERVED)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        payment.cancel();
        log.info("TCC-Cancel: 결제 취소 완료 - orderId: {}", orderId);
    }


    private void validateAmount(Long amount) {
        if (amount < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (amount > 1000000) {
            throw new BusinessException(ErrorCode.PAYMENT_LIMIT_EXCEEDED);
        }
    }

    private void simulateExternalPayment() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
    }
}
